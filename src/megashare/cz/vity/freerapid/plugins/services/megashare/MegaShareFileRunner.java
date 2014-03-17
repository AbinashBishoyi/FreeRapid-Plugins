package cz.vity.freerapid.plugins.services.megashare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Date;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */

/**
 * Request and response data by Tommy[ywx217@gmail.com]
 *  1. First page of http://www.megashare.com/4433199 click download link, then should wait for 10 sec.
 *      Request URL:http://www.megashare.com/4433199
 *      Request Method:POST
 *      Referer:http://www.megashare.com/4433199
 *      Form data:
 *          4433199prZVal:2596608
 *          f£0Dl75314876.x:34
 *          f£0Dl75314876.y:33
 *          f£0Dl75314876:FREE
 *      Cookie:(I'm from China, so geo code is CN, I think there's no need to handle the cookies)
 *          PHPSESSID=c9fbbb5d62rcuo039ade5qq9a6
 *          geoCode=CN
 *
 *  2. After 10 seconds of waiting, it's the download page with captcha image.
 *      Request URL:http://www.megashare.com/4433199
 *      Request Method:POST
 *      Referer:http://www.megashare.com/4433199
 *      Form data:
 *          wComp:1
 *          4433199prZVal:5780228
 *          id:4433199
 *          time_diff:1344232440
 *          req_auth:n
 *      Cookies are the same with the above.
 *      Captcha image URL:
 *          security.php?i=44331991344232451&sid=4433199
 *          Notes:
 *              The parameter "i" is the combination of id and the time_diff value of this page, the time_diff
 *              value(1344232451) is different from the request param time_diff(1344232440) of this page; and
 *              the parameter "sid" is obviously is same with id.
 *
 *  3. Enter the right captcha, start download..
 *      Request URL:http://www.megashare.com/dnd/4433199/bdab773eb22f5212b0384b0c6e7b65b6/r221.zip
 *      Request Method:GET
 *      Referer:http://www.megashare.com/4433199
 *      Form data: no form data.
 *      Cookie:
 *          PHPSESSID=c9fbbb5d62rcuo039ade5qq9a6
 *          geoCode=CN
 *          __atuvc=1%7C32  (domain=www.megashare.com; path=/)
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

            // First stage process.
            HttpMethod httpMethod = processFirstStage();
            if(!makeRedirectedRequest(httpMethod)){
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            logger.info(getContentAsString());

            httpMethod = processSecondStage();
            if(!makeRedirectedRequest(httpMethod)){
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            // Now it's the page contains captcha image.
            httpMethod = stepCaptcha();
            if(tryDownloadAndSaveFile(httpMethod)){
                // break;
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
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
        /**
         *  Captcha image URL:
         *      security.php?i=44331991344232451&sid=4433199
         *      Notes:
         *          The parameter "i" is the combination of id and the time_diff value of this page, the time_diff
         *          value(1344232451) is different from the request param time_diff(1344232440) of this page; and
         *          the parameter "sid" is obviously is same with id.
         */
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String time_diff = getTimeDiff();
        final String fileId = getFileId();
        final String captchaSrc = String.format("http://www.megashare.com/security.php?i=%s&sid=%s",
                fileId + time_diff, fileId);
        logger.info("Captcha URL " + captchaSrc);

        final String captcha;
        // Temporarily disable OCR, because could get the right captcha image.
        if (false && captchaCounter <= CAPTCHA_MAX) {
            //final BufferedImage captchaImage = prepareCaptchaImage(captchaSupport.getCaptchaImage(captchaSrc));
            //captcha = PlugUtils.recognize(captchaImage, "-d -1 -C 0-9");
            captcha = new CaptchaRecognizer().recognize(captchaSupport.getCaptchaImage(captchaSrc));
            logger.info("OCR attempt " + captchaCounter + " of " + CAPTCHA_MAX + ", recognized " + captcha);
            captchaCounter++;
        } else {
            //TODO: I can't get the right captcha image with white characters, because we don't have cookie
            //       __atuvc of megashare.com and uvc of .addthis.com(WTF???), without any of them, captcha image without number...
            //       __atuvc's value may be x%7C32, x is a number usually 1-16, uvc's is the same.
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }

        /**
         *  3. Enter the right captcha, start download..
         *      Request URL:http://www.megashare.com/dnd/4433199/bdab773eb22f5212b0384b0c6e7b65b6/r221.zip
         *      Request Method:GET
         *      Referer:http://www.megashare.com/4433199
         *      Form data: no form data.
         *      Cookie:
         *          PHPSESSID=c9fbbb5d62rcuo039ade5qq9a6
         *          geoCode=CN
         *          __atuvc=1%7C32  (domain=www.megashare.com; path=/)
         */
        return getMethodBuilder()
                .setReferer(fileURL)
                .setAction("http://www.megashare.com/download.php")
                .setParameter("wComp", "1")
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

    private HttpMethod processFirstStage() throws URLNotAvailableAnymoreException, PluginImplementationException {
        if(!getContentAsString().contains("please-scroll.png"))
            throw new URLNotAvailableAnymoreException("Not downloading page");

        final String freeImageParameter = PlugUtils.getStringBetween(getContentAsString(), "name=\"", "\" class=\"textfield\" value=\"FREE\"");
        final String fidParameter = getFileId() + "prZVal";
        final String fidValue = PlugUtils.getParameter(fidParameter, getContentAsString());
        /**
         *  Form data:
         *      4433199prZVal:2596608
         *      f£0Dl75314876.x:34
         *      f£0Dl75314876.y:33
         *      f£0Dl75314876:FREE
         */
        return getMethodBuilder()
                .setAction(fileURL)
                .setReferer(fileURL)
                .setParameter(fidParameter, fidValue)
                .setParameter(freeImageParameter + ".x", Integer.toString(random.nextInt(100)))
                .setParameter(freeImageParameter + ".y", Integer.toString(random.nextInt(100)))
                .setParameter(freeImageParameter, "FREE")
                .toPostMethod();
    }

    private HttpMethod processSecondStage() throws URLNotAvailableAnymoreException, PluginImplementationException {
        final String fid = getFileId();
        final String fidParameter = fid + "prZVal";
        final String fidValue = PlugUtils.getParameter(fidParameter, getContentAsString());
        final long time_diff = new Date().getTime()/1000;
        final String time_diff_str = Long.toString(time_diff);
        /**
         *  Form data:
         *      wComp:1
         *      4433199prZVal:5780228
         *      id:4433199
         *      time_diff:1344232440
         *      req_auth:n
         */
        return getMethodBuilder()
                .setAction(fileURL)
                .setReferer(fileURL)
                .setParameter("wComp", "1")
                .setParameter(fidParameter, fidValue)
                .setParameter("id", fid)
                .setParameter("time_diff", time_diff_str)
                .setParameter("req_auth", "n")
                .toPostMethod();
    }

    private String getFileId() throws URLNotAvailableAnymoreException {
        Matcher fileidMatcher = PlugUtils.matcher("[Mm]ega[Ss]hare\\.com/([0-9]+)", fileURL);
        if(!fileidMatcher.find())
            throw new URLNotAvailableAnymoreException("Cannot get file ID");
        return fileidMatcher.group(1);
    }
    
    private String getTimeDiff() throws PluginImplementationException {
        return PlugUtils.getStringBetween(getContentAsString(), "name=\"time_diff\" value=\"", "\"");
    }
}