package cz.vity.freerapid.plugins.services.ctdisk;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Tommy Yang
 */
class CtdiskRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CtdiskRunner.class.getName());
    private static final int CAPTCHA_MAX = 10;
    private int captchaCounter = 1;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkSizeAndName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkSizeAndName();
            fileURL = method.getURI().toString();
            final String fileId = getFileId();

            while (true) {
                if (!makeRedirectedRequest(stepCaptcha(fileId))) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                if (!getMatcherAgainstContent("验证码输入错误").find()) {
                    if (getContentAsString().contains("You have reached")) {
                        throw new ServiceConnectionProblemException("Free download limit reached");
                    }

                    final String downloadLinkBase64 = PlugUtils.getStringBetween(getContentAsString(), "<a class=\"telecom\" href=\"", "\"");
                    final String downloadLink = new String(Base64.decodeBase64(downloadLinkBase64), "UTF-8");
                    logger.info(String.format("Download link: %s\nDecoded link: %s", downloadLinkBase64, downloadLink));
                    method = getMethodBuilder().setReferer(fileURL).setAction(downloadLink).toGetMethod();
                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                    break;
                } else {
                    makeRedirectedRequest(method);
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkSizeAndName() throws ErrorDuringDownloadingException {
        final Matcher base64InfoMatcher = PlugUtils.matcher("base64\\.decode\\(\"(.+?)\"", getContentAsString());
        if (!base64InfoMatcher.find()) {
            throw new PluginImplementationException("File name and size not found.");
        }
        try {
            final String decodedContent = new String(Base64.decodeBase64(base64InfoMatcher.group(1)), "UTF-8");
//            logger.info("DECODE: " + decodedContent);
            PlugUtils.checkName(httpFile, decodedContent, "target=\"_blank\">", "</a>");
            PlugUtils.checkFileSize(httpFile, decodedContent, "filesize=\"", "\"");
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } catch (UnsupportedEncodingException e) {
            throw new PluginImplementationException("Error decoding content.");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("对不起，这个文件已到期或被删除。") || getContentAsString().contains("404 Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        } else if (getContentAsString().contains("<br />1.") && getContentAsString().contains("<br />2.")) {
            throw new ErrorDuringDownloadingException("Your IP is downloading.");
        }
    }

    private String getFileId() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("/file/([^/]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        return matcher.group(1);
    }

    private HttpMethod stepCaptcha_old(final String fileId) throws Exception {
        String captcha = getCaptchaSupport().getCaptcha(String.format("http://www.ctdisk.com/randcodeV2.php?fid=%s&rand=%f", fileId, 1.0));

        return getMethodBuilder().setReferer(fileURL).setAction("http://www.ctdisk.com/guest_loginV2.php")
                .setParameter("file_id", fileId).setParameter("randcode", captcha).toPostMethod();
    }

    private HttpMethod stepCaptcha(final String fileId) throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaURL = String.format("http://www.ctdisk.com/randcodeV2.php?fid=%s&rand=%f", fileId, 1.0);
        logger.info("Captcha URL " + captchaURL);
        final String captcha;
        String captcha_result;
        if (captchaCounter <= CAPTCHA_MAX) {
            final BufferedImage captchaImage = captchaSupport.getCaptchaImage(captchaURL);
            captcha = PlugUtils.recognize(captchaImage, "-C 0-9");
            if (captcha == null) {
                logger.info("Could not separate captcha letters (attempt " + captchaCounter + " of " + CAPTCHA_MAX + ")");
            }
            captcha_result = captcha.replaceAll("\\D", "");
            logger.info("Attempt " + captchaCounter + " of " + CAPTCHA_MAX + ", OCR recognized " + captcha_result);
            captchaCounter++;
        } else {
            captcha_result = captchaSupport.getCaptcha(captchaURL);
            if (captcha_result == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha_result);
        }
        return getMethodBuilder(getContentAsString())
                .setReferer(fileURL)
                .setActionFromFormByName("user_form", true)
                .setParameter("randcode", captcha_result)
                .toPostMethod();
    }
}