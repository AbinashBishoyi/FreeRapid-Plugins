package cz.vity.freerapid.plugins.services.datacloud;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class DataCloudFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DataCloudFileRunner.class.getName());
    private final static String BASE_URL = "http://datacloud.to";
    private final static int MAX_CAPTCHA_RECOG = 5;
    private int captchaCount = 1;

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h1>Download <b>", "<");
        PlugUtils.checkFileSize(httpFile, content, "Size:</span>", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            checkNameAndSize(getContentAsString());

            final Matcher captchaImageMatch = PlugUtils.matcher("<img.+?\"captcha_img\".+?src=\"(.+?)\"", getContentAsString());
            if (!captchaImageMatch.find())
                throw new ErrorDuringDownloadingException("Captcha image not found");
            final Matcher nextPageMatch = PlugUtils.matcher("<a.+?\"verify_captcha\".+?d_data=\"(.+?)\">.+?</a>", getContentAsString());
            if (!nextPageMatch.find())
                throw new ErrorDuringDownloadingException("Next page not found");
            final String captchaUrl = BASE_URL + captchaImageMatch.group(1);
            final String nextPage = BASE_URL + nextPageMatch.group(1);
            final String strLink = PlugUtils.getStringBetween(getContentAsString(), "var links = '", "';");
            do {
                final String captchaTxt = getCaptcha(captchaUrl);
                MethodBuilder captchaBuilder = getMethodBuilder()
                        .setBaseURL(BASE_URL).setReferer(fileURL)
                        .setAction("/process")
                        .setParameter("action", "checkCapchaDownload")
                        .setParameter("captcha", captchaTxt)
                        .setParameter("link", strLink);
                if (!makeRedirectedRequest(captchaBuilder.toPostMethod())) {
                    throw new ServiceConnectionProblemException();
                }
            } while (!getContentAsString().contains("\"ACTION\":\"OK\""));

            final MethodBuilder nextPageBuilder = getMethodBuilder().setAction(nextPage).setReferer(fileURL);
            if (!makeRedirectedRequest(nextPageBuilder.toGetMethod())) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final MethodBuilder lastPageBuilder = getMethodBuilder().setActionFromAHrefWhereATagContains("Get Link");
            if (!makeRedirectedRequest(lastPageBuilder.toGetMethod())) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final Matcher matcher = getMatcherAgainstContent("<a[^<>]+?href\\s*=\\s*[\"'](.+?)[\"'][^<>]*?>Download</a");
            if (!matcher.find()) {
                throw new PluginImplementationException("Download link not found");
            }
            final String downloadLink = matcher.group(1);
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadLink)
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File does not exist") ||
                contentAsString.contains("Page not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private String getCaptcha(final String captchaUrl) throws FailedToLoadCaptchaPictureException, CaptchaEntryInputMismatchException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaTxt;
        if (captchaCount <= MAX_CAPTCHA_RECOG) {
            captchaTxt = PlugUtils.recognize(binarizeImage(captchaSupport.getCaptchaImage(captchaUrl), 5), "-u 1 -C a-z0-9");
            logger.info(String.format("Captcha auto-recog attempt %d, recognized : %s", captchaCount, captchaTxt));
            captchaCount++;
        } else {
            captchaTxt = captchaSupport.getCaptcha(captchaUrl);
            if (captchaTxt == null) {
                throw new CaptchaEntryInputMismatchException("No Input");
            }
        }
        return captchaTxt;
    }

    private static BufferedImage binarizeImage(final BufferedImage img, final int redLimit) {
        final int w = img.getWidth(), h = img.getHeight();
        final BufferedImage ret = createImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                final int red = (img.getRGB(x, y) >> 16) & 0xFF;
                if (red < redLimit) {
                    ret.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }
        return ret;
    }

    private static BufferedImage createImage(final int w, final int h, final int imgType) {
        final BufferedImage ret = new BufferedImage(w, h, imgType);
        final Graphics g = ret.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        return ret;
    }

}