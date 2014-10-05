package cz.vity.freerapid.plugins.services.keeplinks;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class KeepLinksFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(KeepLinksFileRunner.class.getName());
    private final static int MAX_SIMPLE_CAPTCHA_ATTEMPTS = 3;
    private int simpleCaptchaCount = 1;

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        final int httpStatus = client.makeRequest(getMethod, false);
        if ((httpStatus == 200) || (httpStatus / 100 == 3)) {
            checkProblems();
            httpFile.setFileName("Ready to Extract Link(s)");
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        final String HEADER_LINK_TYPE_1 = ">Direct Link";
        final String HEADER_LINK_TYPE_2 = ">Live Link";
        final int MAX_CAPTCHA_ATTEMPTS = 5;

        List<URI> list = new LinkedList<URI>();

        if (fileURL.contains("/d/")) {
            list.add(stepDirectLink(fileURL));
        } else if (fileURL.contains("/p/")) {
            if (!makeRedirectedRequest(getGetMethod(fileURL))) { //we make the main request
                checkProblems();//check problems
                throw new PluginImplementationException();
            }
            int count = 0;
            MethodBuilder builder;
            String content;
            while (!getContentAsString().contains(HEADER_LINK_TYPE_1) &&
                    !getContentAsString().contains(HEADER_LINK_TYPE_2) &&
                    (count++ < MAX_CAPTCHA_ATTEMPTS)) {
                builder = getMethodBuilder()
                        .setActionFromFormWhereTagContains("Protected link", true)
                        .setReferer(fileURL).setAction(fileURL);
                content = getContentAsString();
                // check 4 & complete captcha
                if (content.contains("Prove you are human")) {
                    stepCaptcha(builder);
                }
                // check 4 & complete password
                if (content.contains("Link password")) {
                    final String password = getDialogSupport().askForPassword("KeepLinks");
                    if (password == null) {
                        throw new PluginImplementationException("This file is secured with a password");
                    }
                    builder.setParameter("link-password", password);
                }
                if (!makeRedirectedRequest(builder.toPostMethod())) { //we make the main request
                    checkProblems();//check problems
                    throw new ServiceConnectionProblemException("err 1");
                }
            }

            if (getContentAsString().contains(HEADER_LINK_TYPE_1) || getContentAsString().contains(HEADER_LINK_TYPE_2)) {
                // all good
            } else if (count >= MAX_CAPTCHA_ATTEMPTS) {
                throw new PluginImplementationException("Excessive Incorrect Captcha Entries");
            } else {
                throw new PluginImplementationException("Captcha Text Error : KeepLinks site changed : KeepLinks feature not supported yet");
            }

            final Matcher m = PlugUtils.matcher("<a href=\"([^\"]+)\"[^>]+?class=\"selecttext (live|direct)", getContentAsString());
            while (m.find()) {
                list.add(encodeUri(m.group(1).trim()));
            }
            if (getContentAsString().contains(HEADER_LINK_TYPE_1)) {
                for (int ii = 0; ii < list.size(); ii++)
                    list.set(ii, stepDirectLink(list.get(ii).toASCIIString()));
            }

        } else {
            checkProblems();
            throw new PluginImplementationException("Invalid link");
        }
        if (list.isEmpty()) throw new PluginImplementationException("No links found");
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.setFileName("Link(s) Extracted !");
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("404 - not found") || content.contains("404 Page/File not found") ||
                content.contains("This link does not exist")) {
            throw new URLNotAvailableAnymoreException("Link does not exist"); //let to know user in FRD
        }
        if (content.contains("server is not currently responding")) {
            throw new ServiceConnectionProblemException("server is not currently responding"); //let to know user in FRD
        }
    }

    private URI stepDirectLink(final String directLinkURL) throws Exception {
        final GetMethod method = getGetMethod(directLinkURL);
        final int httpStatus = client.makeRequest(method, false);
        if (httpStatus / 100 == 3) {
            final Header locationHeader = method.getResponseHeader("Location");
            if (locationHeader == null) throw new PluginImplementationException("Invalid redirect");
            return encodeUri(locationHeader.getValue());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    protected void stepCaptcha(MethodBuilder method) throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captcha;
        if (getContentAsString().contains("recaptcha/api/challenge")) { //recaptcha
            final Matcher m = getMatcherAgainstContent("recaptcha/api/challenge\\?k=(.+?)\"");
            if (!m.find()) throw new PluginImplementationException("Captcha key not found");
            final String captchaKey = m.group(1);
            final ReCaptcha reCaptcha = new ReCaptcha(captchaKey, client);
            captcha = captchaSupport.getCaptcha(reCaptcha.getImageURL());
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            reCaptcha.setRecognized(captcha);
            reCaptcha.modifyResponseMethod(method);
        } else if (getContentAsString().contains("/simplecaptcha/captcha.php")) { //simple captcha
            final String captchaUrl = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromImgSrcWhereTagContains("/simplecaptcha/")
                    .getAction();
            if (simpleCaptchaCount <= MAX_SIMPLE_CAPTCHA_ATTEMPTS) {
                captcha = PlugUtils.recognize(binarizeImage(captchaSupport.getCaptchaImage(captchaUrl), 200), "-u 1 -C a-z0-9");
                logger.info(String.format("Simple captcha auto-recog attempt %d, recognized : %s", simpleCaptchaCount, captcha));
                simpleCaptchaCount++;
            } else {
                captcha = captchaSupport.getCaptcha(captchaUrl);
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException("No Input");
                }
            }
            method.setParameter("norobot", captcha);
        } else {
            throw new PluginImplementationException("Unknown captcha type");
        }
    }

    private URI encodeUri(final String sUri) throws Exception {
        return new URI(URLEncoder.encode(sUri, "UTF-8").replaceAll("%3A", ":").replaceAll("%2F", "/"));
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