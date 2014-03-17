package cz.vity.freerapid.plugins.services.ncrypt;

import cz.vity.freerapid.plugins.container.ContainerPlugin;
import cz.vity.freerapid.plugins.container.ContainerPluginImpl;
import cz.vity.freerapid.plugins.container.FileInfo;
import cz.vity.freerapid.plugins.container.impl.Cnl2;
import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.circlecaptcha.CircleCaptcha;
import cz.vity.freerapid.plugins.services.ncrypt.captcha.CaptchaPreparer;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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
class NcryptFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NcryptFileRunner.class.getName());

    private CircleCaptcha circleCaptcha = null;

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(".ncrypt.in", "SITE_LANGUAGE", "en", "/", 86400, false));

        final HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();

        stepCaptcha();
        final String content = getContentAsString();

        if (!addContainer(content) && !addCnl2(content) && !addWebLinks(content)) {
            throw new PluginImplementationException("No links found");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Your folder does not exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void stepCaptcha() throws Exception {
        while (getContentAsString().contains("captcha")) {
            final MethodBuilder mb = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormByName("protected", true)
                    .setAction(fileURL);
            getCaptcha(mb);
            if (!makeRedirectedRequest(mb.toPostMethod())) {
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private void getCaptcha(final MethodBuilder mb) throws Exception {
        if (getContentAsString().contains("recaptcha")) {
            final String rcKey = PlugUtils.getStringBetween(getContentAsString(), "challenge?k=", "\"");
            final ReCaptcha rc = new ReCaptcha(rcKey, client);
            final String captcha = getCaptchaSupport().getCaptcha(rc.getImageURL());
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            }
            rc.setRecognized(captcha);
            rc.modifyResponseMethod(mb);
        } else if (getContentAsString().contains("anicaptcha")) {
            final String captchaUrl = getMethodBuilder()
                    .setActionFromImgSrcWhereTagContains("anicaptcha")
                    .getEscapedURI();
            final String captcha = getCaptchaSupport().askForCaptcha(getAnicaptchaImage(captchaUrl));
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            }
            mb.setParameter("captcha", captcha);
        } else if (getContentAsString().contains("circlecaptcha")) {
            if (circleCaptcha == null) {
                circleCaptcha = new CircleCaptcha(getDialogSupport(), 12, 25, 0xFFFFFF, 0.8);
            }
            final BufferedImage captchaImage = CaptchaPreparer.getPreparedCirclecaptchaImage(
                    getCaptchaSupport().getCaptchaImage("http://ncrypt.in/classes/captcha/circlecaptcha.php"));
            final Point p = circleCaptcha.recognize(captchaImage);
            if (p != null) {
                mb.setParameter("circle", "Continue+to+folder")
                        .setParameter("circle.x", String.valueOf(p.x))
                        .setParameter("circle.y", String.valueOf(p.y));
            }
        } else {
            throw new PluginImplementationException("Captcha not found");
        }
    }

    private BufferedImage getAnicaptchaImage(final String url) throws Exception {
        final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
        InputStream is = null;
        try {
            is = client.makeRequestForFile(method);
            return CaptchaPreparer.getPreparedAnicaptchaImage(is);
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

    private boolean addContainer(final String content) throws Exception {
        final Matcher matcher = PlugUtils.matcher("href=\"(/container/.+?)\"", content);
        while (matcher.find()) {
            final String url = matcher.group(1);
            final HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(url)
                    .toGetMethod();
            final InputStream is = client.makeRequestForFile(method);
            if (is != null) {
                final ContainerPlugin plugin = ContainerPluginImpl.getInstanceForPlugin(client.getSettings(), getDialogSupport());
                try {
                    if (addContainerLinksToQueue(plugin.read(is, url))) {
                        return true;
                    }
                } catch (final Exception e) {
                    logger.log(Level.WARNING, "Failed to read container", e);
                }
            }
        }
        return false;
    }

    private boolean addCnl2(final String content) throws Exception {
        return addContainerLinksToQueue(Cnl2.read(content));
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
        final Matcher matcher = PlugUtils.matcher("'(http://ncrypt\\.in/link\\-.+?)'", content);
        while (matcher.find()) {
            final String link = matcher.group(1);
            final HttpMethod method = getMethodBuilder()
                    .setReferer(link)
                    .setAction(link.replace("/link-", "/frame-"))
                    .toGetMethod();
            makeRequest(method);
            final String url = getRedirectLocation(method);
            if (url != null && !url.isEmpty()) {
                try {
                    list.add(new URI(url));
                } catch (final URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
        if (!list.isEmpty()) {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
            logger.info(list.size() + " links added");
            return true;
        }
        return false;
    }

    private String getRedirectLocation(final HttpMethod method) {
        final Header header = method.getResponseHeader("Location");
        if (header != null) {
            return header.getValue();
        }
        return null;
    }

}