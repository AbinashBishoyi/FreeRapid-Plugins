package cz.vity.freerapid.plugins.services.megashare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MegaShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MegaShareFileRunner.class.getName());
    private final static Random random = new Random();
    private final static int CAPTCHA_MAX = 10;
    private int captchaCounter = 1;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (isRedirect()) return;
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private boolean isRedirect() {
        return fileURL.contains("r=");
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        // They provide absolutely no info about the file.
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);

        if (makeRedirectedRequest(method)) {
            if (isRedirect()) {
                final String location = PlugUtils.getStringBetween(getContentAsString(), "location.replace('", "');");
                if (location.equalsIgnoreCase("http://www.megashare.com")) {
                    throw new URLNotAvailableAnymoreException("Redirect target not found");
                }
                httpFile.setNewURL(new URL(location));
                httpFile.setPluginID("");
                httpFile.setState(DownloadState.QUEUED);
                return;
            }

            checkProblems();
            checkNameAndSize();

            final String parameter;
            final String value;
            if (getContentAsString().contains("FreePremDz")) {
                logger.info("Free premium");
                parameter = "FreePremDz";
                value = "free+premium";
            } else {
                logger.info("Free download");
                Matcher matcher = getMatcherAgainstContent("FreeDz-\\d+");
                if (!matcher.find()) throw new PluginImplementationException("Free download link not found");
                parameter = matcher.group();
                value = "FREE";
            }

            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(fileURL)
                    .setParameter(parameter, value)
                    .setParameter(parameter + ".x", Integer.toString(random.nextInt(100)))
                    .setParameter(parameter + ".y", Integer.toString(random.nextInt(100)))
                    .toPostMethod();

            if (makeRedirectedRequest(httpMethod)) {
                checkProblems();

                while (true) {
                    httpMethod = stepCaptcha();

                    //waiting is not necessary
                    //downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "var c =", ";") + 1);

                    makeRequest(httpMethod);
                    checkProblems();

                    final Header h = httpMethod.getResponseHeader("Location");
                    if (h != null && h.getValue() != null && !h.getValue().isEmpty()) {
                        httpMethod = getMethodBuilder().setReferer(fileURL).setAction(h.getValue()).toGetMethod();

                        if (tryDownloadAndSaveFile(httpMethod)) {
                            break;
                        } else {
                            checkProblems();
                            throw new ServiceConnectionProblemException("Error starting download");
                        }
                    }
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

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("have a look here")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
        if (content.contains("This file has been DELETED")) {
            final Matcher matcher = getMatcherAgainstContent("<div> (Reason:.+?) </div>");
            if (matcher.find()) {
                throw new URLNotAvailableAnymoreException("This file has been deleted. " + matcher.group(1));
            }
            throw new URLNotAvailableAnymoreException("This file has been deleted");
        }
    }

    private HttpMethod stepCaptcha() throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("security").getEscapedURI();
        logger.info("Captcha URL " + captchaSrc);

        final String captcha;
        if (captchaCounter <= CAPTCHA_MAX) {
            //final BufferedImage captchaImage = prepareCaptchaImage(captchaSupport.getCaptchaImage(captchaSrc));
            //captcha = PlugUtils.recognize(captchaImage, "-d -1 -C 0-9");
            captcha = new CaptchaRecognizer().recognize(captchaSupport.getCaptchaImage(captchaSrc));
            logger.info("OCR attempt " + captchaCounter + " of " + CAPTCHA_MAX + ", recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }

        return getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormByName("downloader", true)
                .setAction(fileURL)
                .setParameter("yesss", "Download")
                .setParameter("yesss.x", Integer.toString(random.nextInt(100)))
                .setParameter("yesss.y", Integer.toString(random.nextInt(100)))
                .setParameter("captcha_code", captcha)
                .toPostMethod();
    }

    private BufferedImage prepareCaptchaImage(final BufferedImage input) {
        final float colorLimit = 7.5f;
        final int w = input.getWidth();
        final int h = input.getHeight();
        final BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        final Graphics g = output.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final float magenta = rgb2cmyk(input.getRGB(x, y))[1];
                final int color = magenta > colorLimit ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
                output.setRGB(x, y, color);
            }
        }
        //JOptionPane.showConfirmDialog(null, new ImageIcon(output));
        return output;
    }

    public static float[] rgb2cmyk(int rgb) {
        float r = (rgb >> 16) & 0xFF;
        float g = (rgb >> 8) & 0xFF;
        float b = rgb & 0xFF;
        float C = 1.0f - (r / 255);
        float M = 1.0f - (g / 255);
        float Y = 1.0f - (b / 255);
        float var_K = 1;

        if (C < var_K) var_K = C;
        if (M < var_K) var_K = M;
        if (Y < var_K) var_K = Y;

        C = (C - var_K) / (1 - var_K);
        M = (M - var_K) / (1 - var_K);
        Y = (Y - var_K) / (1 - var_K);
        return new float[]{C * 100, M * 100, Y * 100, var_K * 100};
    }

    @Override
    protected String getBaseURL() {
        return "http://www.megashare.com/";
    }

}