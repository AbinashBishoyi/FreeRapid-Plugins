package cz.vity.freerapid.plugins.services.linkcrypt;

import cz.vity.freerapid.plugins.container.ContainerPlugin;
import cz.vity.freerapid.plugins.container.ContainerPluginImpl;
import cz.vity.freerapid.plugins.container.FileInfo;
import cz.vity.freerapid.plugins.container.impl.Cnl2;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.circlecaptcha.Circle;
import cz.vity.freerapid.plugins.services.circlecaptcha.CircleCaptcha;
import cz.vity.freerapid.plugins.services.circlecaptcha.CircleHoughTransform;
import cz.vity.freerapid.plugins.services.keycaptcha.KeyCaptcha;
import cz.vity.freerapid.plugins.services.linkcrypt.captcha.CaptchaPreparer;
import cz.vity.freerapid.plugins.services.linkcrypt.captcha.CaptchaRecognizer;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.plugins.webclient.utils.ScriptUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class LinkCryptFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LinkCryptFileRunner.class.getName());

    private final static int CAPTCHA_MAX = 5;
    private int captchaCounter = 0;

    @Override
    public void run() throws Exception {
        super.run();
        setPageEncoding("ISO-8859-1");
        addCookie(new Cookie(".linkcrypt.ws", "language", "en", "/", 86400, false));

        HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        fileURL = method.getURI().toString();
        String content = getContentAsString();

        dummyRequest("http://linkcrypt.ws/js/jquery.js");
        dummyRequest("http://linkcrypt.ws/dir/image/Warning.png");
        dummyRequest("http://linkcrypt.ws/image/menu_right.png");

        if (stepCaptcha(content)) {
            content = getContentAsString();
        }
        if (stepPassword(content)) {
            content = getContentAsString();
        }

        if (!addContainer(content) && !addWebLinks(content)) {
            throw new PluginImplementationException("No links found");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("<title>Linkcrypt.ws // Error 404</title>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void dummyRequest(final String url) throws Exception {
        final InputStream is = client.makeRequestForFile(getGetMethod(url));
        if (is == null) {
            throw new ServiceConnectionProblemException("Dummy request failed");
        }
        try {
            is.close();
        } catch (final Exception e) {
            LogUtils.processException(logger, e);
        }
    }

    private boolean stepPassword(final String content) throws Exception {
        if (content.contains("Enter your password")) {
            do {
                final String password = getDialogSupport().askForPassword("LinkCrypt.ws");
                if (password == null) {
                    throw new PluginImplementationException("This folder is secured with a password");
                }
                final HttpMethod method = getMethodBuilder()
                        .setAction(fileURL).setReferer(fileURL)
                        .setParameter("password", password)
                        .setParameter("x", "" + (int) (Math.random() * 50))
                        .setParameter("y", "" + (int) (Math.random() * 15))
                        .toPostMethod();
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
            } while (getContentAsString().contains("Enter your password"));
            return true;
        }
        return false;
    }

    private boolean stepCaptcha(final String content) throws Exception {
        if (content.contains("KeyCAPTCHA")) {
            final KeyCaptcha kc = new KeyCaptcha(getDialogSupport(), client);
            do {
                final HttpMethod method = kc.recognize(content, fileURL);
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
            } while (getContentAsString().contains("KeyCAPTCHA"));
            return true;
        }
        Matcher matcher = PlugUtils.matcher("src=\"(http://linkcrypt\\.ws/(captx|textx)\\.html.*?)\"", content);
        if (matcher.find()) {
            final String captchaUrl = matcher.group(1);
            final String type = matcher.group(2);
            while (true) {
                final BufferedImage image = getCaptchaImage(captchaUrl);
                final Point p = getCaptcha(image, type);
                if (p != null) {
                    final HttpMethod method = getMethodBuilder()
                            .setReferer(fileURL)
                            .setAction(fileURL)
                            .setParameter("x", String.valueOf(p.x))
                            .setParameter("y", String.valueOf(p.y))
                            .toPostMethod();
                    if (!makeRedirectedRequest(method)) {
                        throw new ServiceConnectionProblemException();
                    }
                    if (!getContentAsString().contains("Warning.png")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private BufferedImage getCaptchaImage(final String url) throws Exception {
        final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
        InputStream is = null;
        try {
            is = client.makeRequestForFile(method);
            return CaptchaPreparer.getPreparedImage(is);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final Exception e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
    }

    private Point getCaptcha(final BufferedImage image, final String type) throws Exception {
        final boolean captx = "captx".equals(type);
        if (captchaCounter < CAPTCHA_MAX) {
            captchaCounter++;
            final Point result;
            if (captx) {
                final CircleHoughTransform cht = new CircleHoughTransform(image, 0xFFFFFF, 12, 22, 1);
                cht.performHoughTransform();
                final Circle circle = cht.findOpenCircle(0.7);
                if (circle != null) {
                    result = new Point(circle.x(), circle.y());
                } else {
                    result = null;
                }
            } else {
                result = CaptchaRecognizer.recognizeTextxCaptcha(image);
            }
            logger.info("Automatic recognition attempt " + captchaCounter + " of " + CAPTCHA_MAX + ": " + result);
            return result;
        } else {
            final String message = captx ? "Please click on the open circle" : "Please click on the circle with the result";
            final Point result = CircleCaptcha.showClickLocationDialog(image, message, getDialogSupport());
            logger.info("Manual recognition: " + result);
            return result;
        }
    }

    private boolean addContainer(final String content) throws Exception {
        final Matcher matcher = PlugUtils.matcher("eval(\\(function\\(p,a,c,k,e,d\\)[^\r\n]+)", content);
        while (matcher.find()) {
            List<FileInfo> list = null;
            final String data = ScriptUtils.evaluateJavaScriptToString(prepareScript(matcher.group(1)));
            if (data.contains("ACTION=\"http://127.0.0.1:9666/")) {
                list = Cnl2.read(data);
            } else {
                final String typeImageUrl = getMethodBuilder(data)
                        .setActionFromImgSrcWhereTagContains("")
                        .getEscapedURI();
                final Matcher matcher1 = PlugUtils.matcher("/image/([a-z]+?)\\.png", typeImageUrl);
                if (!matcher1.find()) {
                    throw new PluginImplementationException("Container type not found");
                }
                final String type = matcher1.group(1);
                final HttpMethod method = getMethodBuilder(data)
                        .setReferer(fileURL)
                        .setActionFromAHrefWhereATagContains("")
                        .toGetMethod();
                final InputStream is = client.makeRequestForFile(method);
                if (is != null) {
                    final ContainerPlugin plugin = ContainerPluginImpl.getInstanceForPlugin(client.getSettings(), getDialogSupport());
                    try {
                        list = plugin.read(is, type);
                    } catch (final Exception e) {
                        logger.log(Level.WARNING, "Failed to read container", e);
                    }
                }
            }
            if (addContainerLinksToQueue(list)) {
                return true;
            }
        }
        return false;
    }

    private String prepareScript(final String script) {
        // The string functions in the JavaScript library don't like \u00AD (SOFT HYPHEN). Workaround:
        return script.replace("\u00AD", "\\xAD");
    }

    private boolean addContainerLinksToQueue(final List<FileInfo> list) {
        if (list != null && !list.isEmpty()) {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueueFromContainer(httpFile, list);
            logger.info(list.size() + " links added");
            return true;
        }
        return false;
    }

    private boolean addWebLinks(final String content) throws Exception {
        final List<URI> list = new LinkedList<URI>();
        final Matcher matcher = PlugUtils.matcher("<input type=\"hidden\" value=\"(.+?)\" name=\"file\"/>", content);
        while (matcher.find()) {
            HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://linkcrypt.ws/out.html")
                    .setParameter("file", matcher.group(1))
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            method = getMethodBuilder()
                    .setReferer(method.getURI().toString())
                    .setActionFromIFrameSrcWhereTagContains("scrolling=\"auto\"")
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            Matcher matcher1 = getMatcherAgainstContent("(?<=\\|)(PGlmcmFtZ[A-Za-z0-9\\+/]+)(?=\\|)");
            final String decoded = new String(Base64.decodeBase64(getLongestString(matcher1)), "UTF-8");
            matcher1 = PlugUtils.matcher("src=\"(.+?)\"", decoded);
            if (!matcher1.find()) {
                throw new PluginImplementationException("Error parsing page (2)");
            }
            final String url = PlugUtils.unescapeHtml(matcher1.group(1));
            try {
                list.add(new URI(url));
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        if (!list.isEmpty()) {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
            logger.info(list.size() + " links added");
            return true;
        }
        return false;
    }

    private String getLongestString(final Matcher matcher) throws ErrorDuringDownloadingException {
        final List<String> list = new LinkedList<String>();
        while (matcher.find()) {
            list.add(matcher.group(1));
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("Error parsing page (1)");
        }
        return Collections.max(list, new Comparator<String>() {
            @Override
            public int compare(final String s1, final String s2) {
                return Integer.valueOf(s1.length()).compareTo(s2.length());
            }
        });
    }

}