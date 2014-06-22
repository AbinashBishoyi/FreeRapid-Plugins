package cz.vity.freerapid.plugins.services.onlinetoshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class OnlineToShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(OnlineToShareFileRunner.class.getName());

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("class=\"descr\">\\s*?<strong>\\s*?(.+?) \\((.+?)\\)\\s*?<br/>\\s*?</strong>", content);
        if (!match.find())
            throw new PluginImplementationException("File name/size not found");
        httpFile.setFileName(match.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String content = getContentAsString();//check for response
            checkProblems();//check problems
            if (content.contains("A password is required to access this file")) {
                do {
                    final String password = getDialogSupport().askForPassword("OnlineToShare");
                    if (password == null)
                        throw new NotRecoverableDownloadException("A password is required to access this file");
                    final HttpMethod passMethod = getMethodBuilder()
                            .setActionFromFormWhereActionContains("file_password", true)
                            .setReferer(fileURL)
                            .setParameter("filePassword", password)
                            .toPostMethod();
                    if (!makeRedirectedRequest(passMethod)) {
                        checkProblems();//if downloading failed
                        throw new ServiceConnectionProblemException("Error submitting password");
                    }
                } while (getContentAsString().contains(">File password is invalid"));
            }
            checkNameAndSize(getContentAsString());//extract file name and size from the page
            final HttpMethod httpMethod = getMethodBuilder()
                    .setActionFromAHrefWhereATagContains("download now")
                    .toGetMethod();
            final int wait = PlugUtils.getNumberBetween(getContentAsString(), "var seconds = ", ";");
            downloadTask.sleep(wait + 1);
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error submitting password");
            }
            boolean captchaLoop = true;
            while (captchaLoop) {
                captchaLoop = false;
                final HttpMethod dlMethod = doCaptcha(getMethodBuilder()
                        .setActionFromFormWhereActionContains(httpFile.getFileName(), true)
                ).toPostMethod();
                if (!tryDownloadAndSaveFile(dlMethod)) {
                    checkProblems();//if downloading failed
                    if (getContentAsString().contains(">Captcha confirmation text is invalid"))
                        captchaLoop = true;
                    else
                        throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            }
        } else {
            checkProblems();
            if (method.getStatusCode() == 404)
                throw new URLNotAvailableAnymoreException("File not found");
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    public MethodBuilder doCaptcha(final MethodBuilder methodBuilder) throws Exception {
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