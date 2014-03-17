package cz.vity.freerapid.plugins.services.filesflash;

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

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Heend
 */
class FilesFlashRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilesFlashRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }


    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<td style=\"text-align:right\">Filename:", "<br />");
        PlugUtils.checkFileSize(httpFile, content, "Size:", "</td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String content = getContentAsString();
            checkProblems();
            checkNameAndSize(content);
            method = getMethodBuilder().setReferer(fileURL).setActionFromFormWhereTagContains("freedownload.php", true).toPostMethod();
            if (makeRedirectedRequest(method)) {
                checkProblems();
                while (getContentAsString().contains("recaptcha")) {
                    stepCaptcha();
                }
                if (getContentAsString().contains("Please Wait")) {
                    final int waitTime = PlugUtils.getWaitTimeBetween(getContentAsString(), "count=", ";", TimeUnit.SECONDS);
                    downloadTask.sleep(waitTime);
                    if (getContentAsString().contains("Click here to start free download")) {
                        method = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Click here to start free download").toPostMethod();
                    }
                }
                if (getContentAsString().contains("Your IP address is already downloading another link.")) {
                    throw new ServiceConnectionProblemException("Free users may only download one file at a time.");
                }
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void stepCaptcha() throws Exception {
        final String publicKey = PlugUtils.getStringBetween(getContentAsString(), ";k=", "\">");
        final MethodBuilder methodBuilder = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("freedownload.php", true);
        final ReCaptcha reCaptcha = new ReCaptcha(publicKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(reCaptcha.getImageURL());
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        reCaptcha.setRecognized(captcha);
        if (!makeRedirectedRequest(reCaptcha.modifyResponseMethod(methodBuilder).toPostMethod())) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }

    }

}