package cz.vity.freerapid.plugins.services.gigasize;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Kajda
 * @since 0.82
 */
class GigaSizeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GigaSizeFileRunner.class.getName());
    private final static String SERVICE_WEB = "http://www.gigasize.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setPageEncoding("ISO-8859-1");
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkSeriousProblems();
            checkNameAndSize();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        setPageEncoding("ISO-8859-1");
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkNameAndSize();

            if (getContentAsString().contains("you must enter the following code below")) {
                int captchaOCRCounter = 1;

                while (getContentAsString().contains("you must enter the following code below")) {
                    httpMethod = stepCaptcha(captchaOCRCounter++);
                    makeRedirectedRequest(httpMethod);
                }

                checkAllProblems();
                //downloadTask.sleep(PlugUtils.getWaitTimeBetween(getContentAsString(), "document.counter.d2.value='", "'", TimeUnit.SECONDS));
                httpMethod = getMethodBuilder().setReferer(SERVICE_WEB + "/formdownload.php").setActionFromFormWhereActionContains("/getcgi.php?t=", true).setBaseURL(SERVICE_WEB).toHttpMethod();

                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkAllProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty");
                }
            } else {
                throw new PluginImplementationException("Captcha form was not found");
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("The file  has been deleted")) {
            throw new URLNotAvailableAnymoreException("The file  has been deleted");
        }

        if (contentAsString.contains("has been removed because we have received a legitimate complaint")) {
            throw new URLNotAvailableAnymoreException("This file has been removed because we have received a legitimate complaint and/or determined that the file violated our Terms of Service");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("YOU HAVE REACHED YOUR HOURLY LIMIT")) {
            throw new YouHaveToWaitException("YOU HAVE REACHED YOUR HOURLY LIMIT", PlugUtils.getWaitTimeBetween(contentAsString, "Please retry after ", " minutes", TimeUnit.MINUTES));
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "Name</strong>: <b>", "<");
        PlugUtils.checkFileSize(httpFile, contentAsString, "Size: <span>", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private HttpMethod stepCaptcha(int captchaOCRCounter) throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = SERVICE_WEB + "/randomImage.php";
        logger.info("Captcha URL " + captchaSrc);
        final String captcha;

        if (captchaOCRCounter <= 3) {
            captcha = readCaptchaImage(captchaSrc);
        } else {
            captcha = captchaSupport.getCaptcha(captchaSrc);
        }

        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            return getMethodBuilder().setReferer(fileURL).setActionFromFormWhereActionContains("/formdownload.php", true).setBaseURL(SERVICE_WEB).setParameter("txtNumber", captcha).toHttpMethod();
        }
    }

    private String readCaptchaImage(String captchaSrc) throws ErrorDuringDownloadingException {
        final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(captchaSrc);
        final BufferedImage croppedCaptchaImage = captchaImage.getSubimage(1, 1, captchaImage.getWidth() - 2, captchaImage.getHeight() - 2);
        String captcha = PlugUtils.recognize(croppedCaptchaImage, "-C A-z-0-9");

        if (captcha != null) {
            logger.info("Captcha - OCR recognized " + captcha);
        } else {
            captcha = "";
        }

        captchaImage.flush();

        return captcha;
    }
}