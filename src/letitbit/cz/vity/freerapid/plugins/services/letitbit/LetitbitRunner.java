package cz.vity.freerapid.plugins.services.letitbit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class LetitbitRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LetitbitRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRequest(httpMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String contentAsString) throws Exception {
        if (!contentAsString.contains("letitbit")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (contentAsString.contains("file was not found")) {
            throw new URLNotAvailableAnymoreException(String.format("The requested file was not found"));

        }
        PlugUtils.checkFileSize(httpFile, contentAsString, "File size::</span>", "</h1>");
        PlugUtils.checkName(httpFile, contentAsString, "File::</span>", "</h1>");

    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        client.getHTTPClient().getParams().setBooleanParameter("dontUseHeaderFilename", true);
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();
        if (makeRedirectedRequest(httpMethod)) {
            checkNameAndSize(getContentAsString());
            boolean useOCR = true;
            while (true) {
                stepCaptcha(useOCR);
                Matcher matcher;

                matcher = PlugUtils.matcher("src=\"([^?]*)\\?link=([^\"]*)\"", getContentAsString());
                if (matcher.find()) {
                    String actionURL = matcher.group(2);
                    String referer = matcher.group(1) + "?link=" + actionURL;
                    final HttpMethod method = getMethodBuilder().
                            setMethodAction(actionURL).
                            setReferer(referer).toGetMethod();
                    logger.info("Download URL: " + actionURL);
                    logger.info(getContentAsString());
                    downloadTask.sleep(4);
                    httpFile.setState(DownloadState.GETTING);
                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        logger.info(getContentAsString());
                        throw new IOException("File input stream is empty.");
                    } else {
                        break;
                    }
                } else {
                    if (getContentAsString().contains("javascript:history.go(-1);")) {
                        logger.info("Wrong captcha");
                        if (useOCR) useOCR = false;
                        makeRedirectedRequest(httpMethod);
                        continue;
                    }
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new PluginImplementationException();
                }
            }
        } else
            throw new PluginImplementationException();
    }

    private void stepCaptcha(boolean useOCR) throws Exception {
        try {
            String captcha;
            if (useOCR) captcha = readCaptchaImage();
            else captcha = getCaptchaSupport().getCaptcha(getCaptchaImageURL());
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            downloadTask.sleep(4);
            HttpMethod postMethod = getMethodBuilder().
                    setActionFromFormWhereActionContains("download", true).
                    setReferer(fileURL).setParameter("cap", captcha).setParameter("fix", "1").
                    toPostMethod();

            if (!makeRequest(postMethod)) {
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }
        } catch (BuildMethodException e) {
            checkProblems();
            logger.warning(e.getMessage());
            throw new InvalidURLOrServiceProblemException("Free download link not found");
        }

    }

    private String getCaptchaImageURL() throws Exception {
        String s = getMethodBuilder().setActionFromImgSrcWhereTagContains("cap.php").getAction();
        logger.info("Captcha - image " + s);
        return s;
    }

    private String readCaptchaImage() throws Exception {
        String s = getCaptchaImageURL();
        String captcha;
        final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(s);
        final BufferedImage croppedCaptchaImage = captchaImage.getSubimage(1, 1, captchaImage.getWidth() - 2, captchaImage.getHeight() - 2);
        captcha = PlugUtils.recognize(croppedCaptchaImage, "-C A-z-0-9");
        if (captcha != null) {
            logger.info("Captcha - OCR recognized " + captcha);
        } else captcha = "";
        captchaImage.flush();//askForCaptcha uvolnuje ten obrazek, takze tady to udelame rucne
        return captcha;

    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        String content = getContentAsString();
        if (content.contains("The page is temporarily unavailable")) {
            throw new YouHaveToWaitException("The page is temporarily unavailable!", 60 * 2);
        }
        if (content.contains("You must have static IP")) {
            throw new YouHaveToWaitException("You must have static IP! Try again", 60 * 2);
        }
        if (content.contains("file was not found")) {
            throw new URLNotAvailableAnymoreException(String.format("The requested file was not found"));

        }

    }


}