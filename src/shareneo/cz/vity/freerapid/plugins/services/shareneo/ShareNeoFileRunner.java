package cz.vity.freerapid.plugins.services.shareneo;

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
class ShareNeoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShareNeoFileRunner.class.getName());

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
        final Matcher match = PlugUtils.matcher("<strong>\\s*?(.+?)\\((.+?)\\)\\s*?<", content);
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
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            final Matcher matchWait = PlugUtils.matcher("var seconds\\s*?=\\s*?(.+?);", contentAsString);
            if (!matchWait.find()) throw new PluginImplementationException("Wait time not found");
            downloadTask.sleep(Integer.parseInt(matchWait.group(1).trim()) + 1);
            final String nextPage = PlugUtils.getStringBetween(contentAsString, ".download-timer').html(\"<a href='", "'>download now</a>\");");
            final int status = client.makeRequest(getGetMethod(nextPage), false);
            if (status / 100 == 3) {
                if (!tryDownloadAndSaveFile(getGetMethod(nextPage))) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            } else if (status != 200) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            boolean incorrectCaptcha;
            do {
                incorrectCaptcha = false;
                final HttpMethod httpMethod = doCaptcha(getMethodBuilder()
                        .setActionFromFormWhereTagContains(httpFile.getFileName(), true)
                        .setReferer(fileURL)
                ).toPostMethod();

                if (!tryDownloadAndSaveFile(httpMethod)) {
                    if (getContentAsString().contains(">Captcha confirmation text is invalid.<"))
                        incorrectCaptcha = true;
                    else {
                        checkProblems();//if downloading failed
                        throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                    }
                }
            } while (incorrectCaptcha);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("if no items, show the original uploader")) {//not found defaults to uploader page
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Delay between downloads must not be less than 30 min")) {
            throw new YouHaveToWaitException("Must wait 30 min between downloads", 30 * 60);
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