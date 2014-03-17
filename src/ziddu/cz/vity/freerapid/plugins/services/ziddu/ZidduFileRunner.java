package cz.vity.freerapid.plugins.services.ziddu;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * @author Kajda+tonyk
 * @since 0.82
 */
class ZidduFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(ZidduFileRunner.class.getName());
    private static final String SERVICE_WEB = "http://downloads.ziddu.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
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
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            System.out.print(getContentAsString());
            checkAllProblems();
            checkNameAndSize();
            final MethodBuilder methodBuilder = getMethodBuilder();
            httpMethod = methodBuilder.setReferer(fileURL).setActionFromFormByName("dfrm", true).toHttpMethod();
            final String redirectURL = methodBuilder.getAction();
            makeRedirectedRequest(httpMethod);
/*
            site throws internal server error (SC_INTERNAL_SERVER_ERROR) at every download page, so condition had to be cancelled
            if (makeRedirectedRequest(httpMethod)){
*/
            int counter = 0;
            String content = getContentAsString();
            while (counter < 5) {
                httpMethod = stepCaptcha(redirectURL, content);
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    if (getContentAsString().contains("captchaform")) {
                        counter++;
                        continue;
                    }
                    checkAllProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty");
                } else break;
            }
/*            } else {
                throw new ServiceConnectionProblemException();
            }
  */
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("Please Enter Correct Verification Code")) {
            throw new YouHaveToWaitException("Please Enter Correct Verification Code", 4);
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "top.document.title=\"Download ", " in Ziddu");
        PlugUtils.checkFileSize(httpFile, contentAsString, "td height=\"18\" align=\"left\" class=\"fontfamilyverdana normal12blue\"><span class=\"fontfamilyverdana normal12black\">", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private HttpMethod stepCaptcha(String redirectURL, String contentAsString) throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        String captchaSrc = SERVICE_WEB + PlugUtils.getStringBetween(contentAsString, "\"", "\" align=\"absmiddle\" id=\"image\" name=\"image\"");
        logger.info("Captcha URL " + captchaSrc);
        captchaSrc = captchaSrc.replaceAll("width=\\d+", "width=150");
        captchaSrc = captchaSrc.replaceAll("height=\\d+", "height=70");
        captchaSrc = captchaSrc.replaceAll("characters=\\d+", "characters=2");

        final BufferedImage image = captchaSupport.getCaptchaImage(captchaSrc);
        ImageProducer producer = new FilteredImageSource(image.getSource(), new RGBGrayFilter(88));
        Image imge = Toolkit.getDefaultToolkit().createImage(producer);

        final BufferedImage bufferedImage = GraphicUtils.toBufferedImage(imge, false);
        String captcha = PlugUtils.recognize(bufferedImage, "-C A-z-0-9");
        //captcha = captchaSupport.getCaptcha(captchaSrc);
        logger.info("Recognized captcha " + captcha);

//        captchaSupport.askForCaptcha(bufferedImage);

        if (captcha == null || captcha.isEmpty() || captcha.length() != 2)
            captcha = captchaSupport.getCaptcha(captchaSrc);
        else captcha = captcha.toLowerCase(Locale.ENGLISH);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            return getMethodBuilder(contentAsString)
                    .setReferer(redirectURL)
                    .setActionFromFormByName("securefrm", true)
                    .setBaseURL(SERVICE_WEB).setParameter("securitycode", captcha)
                    .setParameter("accelerator", "").toHttpMethod();
        }
    }

    private static class RGBGrayFilter extends RGBImageFilter {
        private final int limit;
        //        private final boolean letGray;
        private static final int whiteRGB = new Color(255, 255, 255, 255).getRGB();
//        private static final int blackRGB = new Color(0, 0, 0, 255).getRGB();

        public RGBGrayFilter(int limit) {
            this.limit = limit;
            canFilterIndexColorModel = true;
        }

        public int filterRGB(int x, int y, int rgb) {
            int a = rgb & 0xff000000;
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
//		rgb = (r + g + b) / 3;	// simple average
            rgb = (r * 77 + g * 151 + b * 28) >> 8;    // NTSC luma

            if ((rgb & 0xFF) > limit) {
                return whiteRGB;
            } else {
                return a | (rgb << 16) | (rgb << 8) | rgb;
            }
        }
    }


}