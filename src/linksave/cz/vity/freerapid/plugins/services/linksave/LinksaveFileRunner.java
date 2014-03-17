package cz.vity.freerapid.plugins.services.linksave;

import cz.vity.freerapid.plugins.container.ContainerPlugin;
import cz.vity.freerapid.plugins.container.ContainerPluginImpl;
import cz.vity.freerapid.plugins.container.FileInfo;
import cz.vity.freerapid.plugins.container.impl.Cnl2;
import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.linksave.captcha.CaptchaPreparer;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

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
 * Class which contains main code
 *
 * @author Arthur Gunawan, ntoskrnl
 */
class LinksaveFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LinksaveFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".linksave.in", "Linksave_Language", "english", "/", 86400, false));

        loadPageWithCaptcha(fileURL);
        checkProblems();

        final String content = getContentAsString();
        if (!addContainerLinks(content) && !addCnl2Links(content) && !addPlaintextLinks(content) && !addWebProtectionLinks(content)) {
            throw new PluginImplementationException("No links found");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Folder not found") || content.contains("<h1>404 - Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("Folder not found");
        }
    }

    private boolean addContainerLinksToQueue(final List<FileInfo> list) {
        if (list != null && !list.isEmpty()) {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueueFromContainer(httpFile, list);
            logger.info(list.size() + " links added");
            httpFile.getProperties().put("removeCompleted", true);
            return true;
        }
        return false;
    }

    private boolean addLinksToQueue(final List<URI> list) {
        if (list != null && !list.isEmpty()) {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
            logger.info(list.size() + " links added");
            httpFile.getProperties().put("removeCompleted", true);
            return true;
        }
        return false;
    }

    private boolean addContainerLinks(final String content) throws Exception {
        final Matcher matcher = PlugUtils.matcher("getElementById\\('[^']+?_link'\\)\\.href=unescape\\('([^']+?)'\\);", content);
        while (matcher.find()) {
            final String url = URLDecoder.decode(matcher.group(1), "UTF-8");
            final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
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

    private boolean addCnl2Links(final String content) {
        return addContainerLinksToQueue(Cnl2.read(content));
    }

    private boolean addPlaintextLinks(final String content) {
        final Matcher matcher = PlugUtils.matcher("(?s)<textarea[^<>]*>(.+?)</textarea>", content);
        if (matcher.find()) {
            final String found = matcher.group(1).trim();
            final String[] split = found.split("\\s+");

            final List<URI> list = new LinkedList<URI>();
            for (String link : split) {
                link = link.trim();
                if (!link.isEmpty()) {
                    try {
                        list.add(new URI(link));
                    } catch (final URISyntaxException e) {
                        LogUtils.processException(logger, e);
                    }
                }
            }
            return addLinksToQueue(list);
        }
        return false;
    }

    private boolean addWebProtectionLinks(String content) throws Exception {
        final List<URI> list = new LinkedList<URI>();
        int page = 1;
        do {
            if (page > 1) {
                loadPageWithCaptcha(fileURL + "?s=" + page + "#down");
                content = getContentAsString();
            }
            final Matcher matcher = PlugUtils.matcher("href=\"(http://.+?)\" onclick=\"javascript:", content);
            if (matcher.find()) {
                do {
                    final String link = unWebProtect(matcher.group(1));
                    try {
                        list.add(new URI(link));
                    } catch (final URISyntaxException e) {
                        LogUtils.processException(logger, e);
                    }
                } while (matcher.find());
            } else {
                break;
            }
        } while (content.contains(">[" + ++page + "]<"));

        return addLinksToQueue(list);
    }

    private String unWebProtect(final String url) throws Exception {
        HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        method = getMethodBuilder().setReferer(method.getURI().toString()).setActionFromIFrameSrcWhereTagContains("scrolling=\"auto\"").toGetMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }

        if (getContentAsString().contains("IIIIIl(\"")) {
            final String toUnescape = PlugUtils.getStringBetween(getContentAsString(), "IIIIIl(\"", "\"");
            final String unescaped = URLDecoder.decode(toUnescape, "UTF-8");
            final String toDecrypt = PlugUtils.getStringBetween(unescaped, "a('", "')");
            final String decrypted = new String(Base64.decodeBase64(toDecrypt), "UTF-8");

            final Matcher matcher = PlugUtils.matcher("(?:location\\.replace\\('|src=\"|URL=)(.+?)['\"\\)]", decrypted);
            if (!matcher.find()) {
                throw new PluginImplementationException("Problem with final download link");
            }

            method = getMethodBuilder(decrypted).setReferer(method.getURI().toString()).setAction(matcher.group(1)).toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
        }

        final String redirect = method.getURI().toString();
        if (!redirect.matches("http://(?:www\\.)?linksave\\.in/.+")) {
            return redirect;
        }

        final String src = PlugUtils.getStringBetween(getContentAsString(), "<iframe src=\"", "\"");
        return PlugUtils.unescapeHtml(src);
    }

    private void loadPageWithCaptcha(final String pageUrl) throws Exception {
        final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(pageUrl).toGetMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        while (getContentAsString().contains("Captcha:")) {
            if (!makeRedirectedRequest(stepCaptcha(pageUrl))) {
                throw new ServiceConnectionProblemException();
            }
            if (getContentAsString().contains("Wrong code")) {
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
            }
        }
    }

    private HttpMethod stepCaptcha(final String pageUrl) throws Exception {
        final String captchaURL = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getEscapedURI();
        logger.info("Captcha URL " + captchaURL);

        final BufferedImage captchaImage;
        try {
            final HttpMethod method = getMethodBuilder().setReferer(pageUrl).setAction(captchaURL).toGetMethod();
            captchaImage = CaptchaPreparer.getPreparedImage(client.makeRequestForFile(method));
        } catch (final Exception e) {
            LogUtils.processException(logger, e);
            return getGetMethod(fileURL);
        }

        final String captcha = getCaptchaSupport().askForCaptcha(captchaImage);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        logger.info("Manual captcha " + captcha);

        return getMethodBuilder()
                .setReferer(pageUrl)
                .setBaseURL(pageUrl)
                .setActionFromFormWhereTagContains("captcha", true)
                .setParameter("code", captcha)
                .toPostMethod();
    }

}
