package cz.vity.freerapid.plugins.services.datafile;

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
class DataFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DataFileFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "file-name\">", "</div>");
        PlugUtils.checkFileSize(httpFile, content, "Filesize:<span class=\"lime\">", "</span>");
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

            MethodBuilder builder = getMethodBuilder()
                    .setActionFromFormWhereTagContains("recaptcha", true)
                    .setAjax()
                    .setAction("https://www.datafile.com/files/ajax.html")
                    .setReferer(fileURL)
                    .setParameter("doaction", "getFileDownloadLink")
                    .setParameter("fileid", PlugUtils.getStringBetween(getContentAsString(), "getFileDownloadLink('", "'"));
            boolean captchaLoop = false;
            do {
                if (captchaLoop) {
                    if (!makeRedirectedRequest(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();
                }
                //final int waitTime = 1 + PlugUtils.getNumberBetween(getContentAsString(), "counter.contdownTimer('", "'");
                //final long startTime = System.currentTimeMillis();
                doCaptcha(builder);
                //final long endTime = System.currentTimeMillis();
                //downloadTask.sleep(waitTime - (int)((endTime-startTime)/1000));
                setFileStreamContentTypes(new String[0], new String[]{"application/x-www-form-urlencoded"});
                if (!makeRedirectedRequest(builder.toPostMethod())) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                captchaLoop = true;
            } while (!getContentAsString().contains("success\":1"));

            final String url = PlugUtils.getStringBetween(getContentAsString(), "link\":\"", "\"").replace("\\/", "/");
            client.getHTTPClient().getParams().setBooleanParameter("dontUseHeaderFilename", true);
            final HttpMethod httpMethod = getGetMethod(url);
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }


    private void doCaptcha(MethodBuilder methodBuilder) throws Exception {
        final String reCaptchaKey = PlugUtils.getStringBetween(getContentAsString(), "recaptcha/api/challenge?k=", "\"");
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);
        r.modifyResponseMethod(methodBuilder);
    }

}