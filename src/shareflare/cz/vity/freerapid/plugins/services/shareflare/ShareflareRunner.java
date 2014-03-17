package cz.vity.freerapid.plugins.services.shareflare;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpMethod;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.services.shareflare.captcha.CaptchaReader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

/**
 * @author RickCL
 */
class ShareflareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShareflareRunner.class.getName());

    private int captchatry = 0;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();

        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "<p id=\"file-info\">", "<small>");
        PlugUtils.checkFileSize(httpFile, contentAsString, "<p id=\"file-info\">" + httpFile.getFileName()
                + " <small>(", ")</small>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        final HttpMethod httpMethod2 = getMethodBuilder().setActionFromFormWhereTagContains("dvifree", true)
                .toPostMethod();
        if (!makeRedirectedRequest(httpMethod2)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        final MethodBuilder methodBuilder = getMethodBuilder().setActionFromFormWhereTagContains("dvifree", true);

        String captchaurl = getCaptchaImageURL();
        do {
            String captcha = readCaptchaImage( captchaurl );

            methodBuilder.setParameter("cap", captcha);
            if (!makeRedirectedRequest(methodBuilder.toPostMethod())) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            captchatry++;
        } while( getContentAsString().contains("history.go(-1)") && captchatry < 3 );
        if( captchatry == 3 )
            throw new CaptchaEntryInputMismatchException("Captcha Error");

        final HttpMethod httpMethod3 = getMethodBuilder().setActionFromIFrameSrcWhereTagContains("topFrame")
                .toGetMethod();
        if (!makeRedirectedRequest(httpMethod3)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "<span id=\"errt\">", "</span>") + 1);

        if (!makeRedirectedRequest(httpMethod3)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        if (!getContentAsString().contains("Your link to file download"))
            throw new PluginImplementationException("Some waiting problem");

        final HttpMethod httpMethod4 = getMethodBuilder()
            .setActionFromAHrefWhereATagContains("Your link to file download")
                .setReferer(methodBuilder.getAction()).toGetMethod();
        if (!tryDownloadAndSaveFile(httpMethod4)) {
            checkProblems();
            logger.warning(getContentAsString());
            throw new IOException("File input stream is empty");
        }
    }

    private String readCaptchaImage(String captchaurl) throws Exception {
        final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(captchaurl);
        String captcha = null;
        if (captchatry == 0) {
            CaptchaReader cr = new CaptchaReader(captchaImage);
            captcha = cr.getWord();
            if (captcha != null) {
                logger.info("Captcha - RickCL Captcha recognized: " + captcha);
            } else
                captchatry = 1;
        }
        if (captchatry == 1) {
            final BufferedImage croppedCaptchaImage = captchaImage.getSubimage(1, 1, captchaImage.getWidth() - 2,
                    captchaImage.getHeight() - 2);
            captcha = PlugUtils.recognize(croppedCaptchaImage, "-C a-z-0-9");
            if (captcha != null) {
                logger.info("Captcha - OCR recognized: " + captcha);
            } else
                captchatry = 2;
        }
        if (captchatry == 2) {
            captcha = getCaptchaSupport().askForCaptcha(captchaImage);
        }
        if (captcha == null)
            throw new CaptchaEntryInputMismatchException();
        return captcha;
    }

    private String getCaptchaImageURL() throws Exception {
        String s = getMethodBuilder().setActionFromImgSrcWhereTagContains("cap.php").getAction();
        logger.info("Captcha Image: " + s);
        return s;
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException,
            URLNotAvailableAnymoreException {
        final String content = getContentAsString();
        if (content.contains("Error")) {
            throw new YouHaveToWaitException("The page is temporarily unavailable", 60 * 2);
        }
        if (content.contains("File not found") || content.contains("deleted for abuse") ) {
            throw new URLNotAvailableAnymoreException("The requested file was not found");
        }
    }

}