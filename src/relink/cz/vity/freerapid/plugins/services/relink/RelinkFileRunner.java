package cz.vity.freerapid.plugins.services.relink;

import cz.vity.freerapid.plugins.container.ContainerPlugin;
import cz.vity.freerapid.plugins.container.ContainerPluginImpl;
import cz.vity.freerapid.plugins.container.FileInfo;
import cz.vity.freerapid.plugins.container.impl.Cnl2;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.circlecaptcha.CircleCaptcha;
import cz.vity.freerapid.plugins.services.relink.captcha.CaptchaPreparer;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author benpicco
 * @author ntoskrnl
 */
class RelinkFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RelinkFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        fileURL = fileURL.replaceFirst("/(view|container_captcha)\\.php\\?id=", "/f/");

        final HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        if (!method.getURI().toString().contains("relink.us")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        fileURL = method.getURI().toString();

        stepCaptcha();
        final String content = getContentAsString();

        if (!addContainer(content) && !addCnl2(content) && !addWebLinks(content)) {
            throw new PluginImplementationException("No links found");
        }
    }

    private void stepCaptcha() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("src=\"(core/captcha/.+?)\"");
        if (matcher.find()) {
            final MethodBuilder mb = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormByName("form", true);
            final String captchaUrl = getMethodBuilder()
                    .setAction(matcher.group(1))
                    .getEscapedURI();
            final CircleCaptcha captcha = new CircleCaptcha(getDialogSupport(), 12, 28, 0xFFFFFF, 0.8);
            while (true) {
                final BufferedImage captchaImage = CaptchaPreparer.getPreparedImage(
                        getCaptchaSupport().getCaptchaImage(captchaUrl));
                final Point p = captcha.recognize(captchaImage);
                if (p != null) {
                    mb.setParameter("button", "Send")
                            .setParameter("button.x", String.valueOf(p.x))
                            .setParameter("button.y", String.valueOf(p.y));
                    final HttpMethod method = mb.toPostMethod();
                    if (!makeRedirectedRequest(method)) {
                        throw new ServiceConnectionProblemException();
                    }
                    if (!getContentAsString().contains("<p class=\"msg_error\">")) {
                        fileURL = method.getURI().toString();
                        return;
                    }
                }
            }
        }
    }

    private boolean addContainer(final String content) throws Exception {
        final Matcher matcher = PlugUtils.matcher("<a href=\"(download\\.php\\?.+?)\" target=\"_blank\"><img src=\"images/([a-z]+?)\\.jpg\"", content);
        while (matcher.find()) {
            final HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(matcher.group(1))
                    .toGetMethod();
            final InputStream is = client.makeRequestForFile(method);
            if (is != null) {
                final ContainerPlugin plugin = ContainerPluginImpl.getInstanceForPlugin(client.getSettings(), getDialogSupport());
                try {
                    if (addContainerLinksToQueue(plugin.read(is, matcher.group(2)))) {
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
        Matcher matcher = PlugUtils.matcher("<param name=\"flashVars\" value=\"(.+?)\"", content);
        if (matcher.find()) {
            final String flashVars = matcher.group(1);
            matcher = PlugUtils.matcher("jk=([^&]+)", flashVars);
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing flashVars (1)");
            }
            final String jk = URLDecoder.decode(matcher.group(1), "UTF-8");
            matcher = PlugUtils.matcher("crypted=([^&]+)", flashVars);
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing flashVars (2)");
            }
            final String crypted = matcher.group(1);
            return addContainerLinksToQueue(Cnl2.decrypt(crypted, Cnl2.executeKeyJs(jk)));
        }
        return false;
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
        final Matcher matcher = PlugUtils.matcher("getFile\\('(.+?)'\\)", content);
        while (matcher.find()) {
            Thread.sleep(1000);
            final HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("frame.php?" + matcher.group(1))
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final String url = getMethodBuilder()
                    .setActionFromIFrameSrcWhereTagContains("Container")
                    .getEscapedURI();
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

}
