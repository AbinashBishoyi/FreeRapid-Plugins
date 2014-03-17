package cz.vity.freerapid.plugins.services.shareflare;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.shareflare.captcha.CaptchaReader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * @author RickCL, ntoskrnl
 */
class ShareflareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShareflareRunner.class.getName());

    private int captchatry = 0;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".shareflare.net", "lang", "en", "/", 86400, false));
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        PlugUtils.checkName(httpFile, getContentAsString(), "<p>File:", "</p>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<p>Size:", "</p>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".shareflare.net", "lang", "en", "/", 86400, false));

        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkNameAndSize();

        final HttpMethod httpMethod2 = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("dvifree", true).toPostMethod();
        if (!makeRedirectedRequest(httpMethod2)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        final MethodBuilder methodBuilder = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("dvifree", true);
        final String captchaurl = getCaptchaImageURL();
        do {
            final HttpMethod captchaMethod = methodBuilder.setParameter("cap", readCaptchaImage(captchaurl)).toPostMethod();
            if (!makeRedirectedRequest(captchaMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            captchatry++;
        } while (getContentAsString().contains("history.go(-1)"));

        final HttpMethod httpMethod3 = getMethodBuilder().setReferer(methodBuilder.getEscapedURI()).setActionFromIFrameSrcWhereTagContains("name=\"topFrame\"").toGetMethod();
        if (!makeRedirectedRequest(httpMethod3)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "<span id=\"errt\">", "</span>") + 1);

        if (!makeRedirectedRequest(httpMethod3)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        final HttpMethod httpMethod4 = getMethodBuilder()
                .setActionFromAHrefWhereATagContains("Your link to file download")
                .setReferer(methodBuilder.getEscapedURI())
                .toGetMethod();
        if (!tryDownloadAndSaveFile(httpMethod4)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private String readCaptchaImage(final String captchaurl) throws Exception {
        final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(captchaurl);
        String captcha;
        switch (captchatry) {
            case 0:
                CaptchaReader cr = new CaptchaReader(captchaImage);
                captcha = cr.getWord();
                if (captcha != null) {
                    logger.info("Captcha - RickCL Captcha recognized: " + captcha);
                    break;
                }
            case 1:
                final BufferedImage croppedCaptchaImage = captchaImage.getSubimage(1, 1, captchaImage.getWidth() - 2, captchaImage.getHeight() - 2);
                captcha = PlugUtils.recognize(croppedCaptchaImage, "-C a-z-0-9");
                if (captcha != null) {
                    logger.info("Captcha - OCR recognized: " + captcha);
                    break;
                }
            default:
                captcha = getCaptchaSupport().askForCaptcha(captchaImage);
                if (captcha != null) {
                    logger.info("Captcha - Manual: " + captcha);
                }
                break;
        }
        if (captcha == null)
            throw new CaptchaEntryInputMismatchException();
        return captcha;
    }

    private String getCaptchaImageURL() throws ErrorDuringDownloadingException {
        final String s = getMethodBuilder().setActionFromImgSrcWhereTagContains("cap.php").getEscapedURI();
        logger.info("Captcha image: " + s);
        return s;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        /* May produce false positives, eg. if the filename contains the word "Error".
        if (content.contains("Error")) {
            throw new ServiceConnectionProblemException("The page is temporarily unavailable");
        }*/
        if (content.contains("File not found") || content.contains("deleted for abuse") || content.contains("<h1>404 Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("The requested file was not found");
        }
    }

    @Override
    protected String getBaseURL() {
        return "http://shareflare.net";
    }

}