package cz.vity.freerapid.plugins.services.ctdisk;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Tommy Yang
 */
class CtdiskRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CtdiskRunner.class.getName());
    private static final int CAPTCHA_MAX = 10;
    private int captchaCounter = 1;
    private String pageContentWithCaptcha;
    private Random random = new Random();

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
            pageContentWithCaptcha = getContentAsString();
            while (!(getContentAsString().contains("downlink=") || getContentAsString().contains("<a class=\"local\""))) {
                if (!makeRedirectedRequest(stepCaptcha(fileId))) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            }
            if (getContentAsString().contains("downlink=")) {
                if (!makeRedirectedRequest(getGetMethod(getUrlRoot() + PlugUtils.getStringBetween(getContentAsString(), "downlink=/", "\";")))) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            }
            final String downloadLinkBase64 = PlugUtils.getStringBetween(getContentAsString(), "<a class=\"local\" href=\"", "\"");
            final String downloadLink = new String(Base64.decodeBase64(downloadLinkBase64), "UTF-8");
            logger.info(String.format("Download link: %s\nDecoded link: %s", downloadLinkBase64, downloadLink));
            method = getMethodBuilder().setReferer(fileURL).setAction(downloadLink).toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkSizeAndName() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<h1>", "<");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<small>", "</");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("对不起，这个文件已到期或被删除。") || getContentAsString().contains("404 Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("<li>1.") && getContentAsString().contains("<li>3.") && getContentAsString().contains("<li>4.")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("<br />1.") && getContentAsString().contains("<br />2.")) {
            throw new ErrorDuringDownloadingException("Your IP is downloading.");
        }
        if (getContentAsString().contains("You have reached")) {
            throw new ServiceConnectionProblemException("Free download limit reached");
        }
    }

    private String getFileId() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("/file/([^/]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        return matcher.group(1);
    }

    private HttpMethod stepCaptcha(final String fileId) throws Exception {
        final String urlRoot = getUrlRoot();
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaURL = String.format("%srandcodeV2_login.php?fid=%s&rand=%f", urlRoot, fileId, Math.random());
        logger.info("Captcha URL " + captchaURL);
        final String captcha;
        String captcha_result;
        if (captchaCounter <= CAPTCHA_MAX) {
            final BufferedImage captchaImage = captchaSupport.getCaptchaImage(captchaURL);
            captcha = PlugUtils.recognize(captchaImage, "-C 0-9");
            if (captcha == null) {
                logger.info("Could not separate captcha letters (attempt " + captchaCounter + " of " + CAPTCHA_MAX + ")");
                captcha_result = Integer.toString(random.nextInt(8000) + 1000);
            } else {
                captcha_result = captcha.replaceAll("\\D", "");
                // There should be 4 digital chars, if not then they'll be the default random value.
                if (captcha_result.length() != 4) {
                    logger.info("Captcha length is not 4, randomizing captcha..");
                    captcha_result = Integer.toString(random.nextInt(8000) + 1000);
                }
            }

            logger.info("Attempt " + captchaCounter + " of " + CAPTCHA_MAX + ", OCR recognized " + captcha_result);
            captchaCounter++;
        } else {
            captcha_result = captchaSupport.getCaptcha(captchaURL);
            if (captcha_result == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha_result);
        }

        return getMethodBuilder(pageContentWithCaptcha)
                .setReferer(fileURL)
                .setActionFromFormByName("user_form", true)
                .setAction(String.format("%sguest_loginV2.php", urlRoot))
                .setParameter("randcode", captcha_result)
                .setParameter("hash_key", new String(Base64.decodeBase64(PlugUtils.getStringBetween(pageContentWithCaptcha, "hash_info\" value=\"", "\""))))
                .toPostMethod();
    }

    private String getUrlRoot() {
        if (fileURL.contains("ctdisk"))
            return "http://www.ctdisk.com/";
        else if (fileURL.contains("400gb"))
            return "http://www.400gb.com/";
        else if (fileURL.contains("t00y"))
            return "http://www.t00y.com/";
        else if (fileURL.contains("bego.cc"))
            return "http://www.bego.cc/";
        else
            return "http://www.pipipan.com/";

    }
}