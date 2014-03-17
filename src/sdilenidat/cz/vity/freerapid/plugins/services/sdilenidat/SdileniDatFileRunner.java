package cz.vity.freerapid.plugins.services.sdilenidat;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class SdileniDatFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SdileniDatFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "zev souboru: </strong>", "<br");
        PlugUtils.checkFileSize(httpFile, content, "Velikost souboru: </strong>", "<br");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("SlowSpeed").toHttpMethod();
            boolean captchaLoop = true;
            while (captchaLoop) {
                captchaLoop = false;
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                final HttpMethod dlMethod = doCaptcha(getMethodBuilder()
                        .setActionFromFormWhereTagContains("stahnout", true))
                        .setAction(httpMethod.getURI().getURI())
                        .toPostMethod();

                setFileStreamContentTypes(new String[0], new String[]{"application/x-www-form-urlencoded"});
                if (!tryDownloadAndSaveFile(dlMethod)) {
                    checkProblems();//if downloading failed
                    if (getContentAsString().contains("Ověřovací kod nebyl zadán správně"))
                        captchaLoop = true;
                    else
                        throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Stránka kterou hledáte se tady nenachází") ||
                contentAsString.contains("Soubor na serveru neexistuje") ||
                contentAsString.contains("odkaz na stažení není platný.")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Could not open socket")) {
            throw new ServiceConnectionProblemException("Could not open socket");
        }
    }

    private MethodBuilder doCaptcha(MethodBuilder methodBuilder) throws Exception {
        final String reCaptchaKey = PlugUtils.getStringBetween(getContentAsString(), "recaptcha/api/challenge?k=", "\"");
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);
        r.modifyResponseMethod(methodBuilder);
        return methodBuilder;
    }
}