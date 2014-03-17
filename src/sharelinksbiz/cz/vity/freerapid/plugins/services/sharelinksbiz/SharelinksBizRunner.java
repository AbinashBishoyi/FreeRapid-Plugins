package cz.vity.freerapid.plugins.services.sharelinksbiz;

import cz.vity.freerapid.plugins.container.ContainerPlugin;
import cz.vity.freerapid.plugins.container.ContainerPluginImpl;
import cz.vity.freerapid.plugins.container.FileInfo;
import cz.vity.freerapid.plugins.container.impl.Cnl2;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import cz.vity.freerapid.utilities.Utils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author RickCL
 * @author ntoskrnl
 */
class SharelinksBizRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SharelinksBizRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(".share-links.biz", "SLlng", "en", "/", 86400, false));

        HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        if (getContentAsString().contains("<meta HTTP-EQUIV=\"refresh\"")) {
            final Matcher matcher = getMatcherAgainstContent("<meta HTTP-EQUIV=\"refresh\" CONTENT=\"1; URL=(.+?)\"");
            if (!matcher.find()) {
                throw new ServiceConnectionProblemException("Redirect location not found");
            }
            method = getMethodBuilder()
                    .setReferer(method.getURI().toString())
                    .setAction(matcher.group(1))
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
        checkProblems();
        fileURL = method.getURI().toString();

        stepCaptcha();
        final String content = getContentAsString();

        method = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("http://share-links.biz/template/images/header/blank.gif")
                .toGetMethod();
        requestImage(method);

        if (!addContainer(content) && !addCnl2(content) && !addWebLinks(content)) {
            throw new PluginImplementationException("No links found");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("No usable content was found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void requestImage(final HttpMethod method) {
        try {
            final InputStream is = client.makeRequestForFile(method);
            if (is != null) {
                is.close();
            }
        } catch (final Exception e) {
            LogUtils.processException(logger, e);
        }
    }

    private void stepCaptcha() throws Exception {
        if (getContentAsString().contains("captcha.gif")) {
            final Matcher images = getMatcherAgainstContent("(/captcha.gif[^\"]*)");
            final Matcher options = getMatcherAgainstContent("<area[^<>]*?href=\"([^\"]+?)\"");
            while (images.find()) {
                final HttpMethod method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(PlugUtils.replaceEntities(images.group(1)))
                        .toGetMethod();
                requestImage(method);
            }
            while (options.find()) {
                final HttpMethod method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(options.group(1))
                        .toGetMethod();
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
                if (!getContentAsString().contains("Your choice was wrong")) {
                    return;
                }
            }
            throw new PluginImplementationException("Cannot solve captcha");
        }
    }

    private boolean addContainerLinksToQueue(final List<FileInfo> list) {
        if (list != null && !list.isEmpty()) {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueueFromContainer(httpFile, list);
            logger.info(list.size() + " links added");
            return true;
        }
        return false;
    }

    private boolean addContainer(final String content) throws Exception {
        final Matcher matcher = PlugUtils.matcher("javascript:_get\\('([^']+)',\\s*\\d+,\\s*'([^']+)'\\);", content);
        while (matcher.find()) {
            final String type = matcher.group(2);
            final HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("/get/" + type + "/" + matcher.group(1))
                    .toGetMethod();
            final InputStream is = client.makeRequestForFile(method);
            if (is != null) {
                final ContainerPlugin plugin = ContainerPluginImpl.getInstanceForPlugin(client.getSettings(), getDialogSupport());
                try {
                    if (addContainerLinksToQueue(plugin.read(is, type))) {
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
        final Matcher matcher = PlugUtils.matcher("ClicknLoad\\.swf\\?code=(.+?)\"", content);
        if (matcher.find()) {
            final HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("/get/cnl2/" + matcher.group(1))
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final String[] split = getContentAsString().split(";;");
            if (split.length < 3) {
                throw new PluginImplementationException("Error parsing CNL2 data");
            }
            final String key = Utils.reverseString(new String(Base64.decodeBase64(split[1]), "UTF-8"));
            final String data = Utils.reverseString(new String(Base64.decodeBase64(split[2]), "UTF-8"));
            return addContainerLinksToQueue(Cnl2.decrypt(data, Cnl2.executeKeyJs(key)));
        }
        return false;
    }

    private boolean addWebLinks(final String content) throws Exception {
        final List<URI> list = new LinkedList<URI>();
        final Matcher matcher = PlugUtils.matcher("javascript:_get\\('([^']+)',\\s*\\d+,\\s*''\\);", content);
        while (matcher.find()) {
            HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("/get/lnk/" + matcher.group(1))
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            method = getMethodBuilder()
                    .setReferer(method.getURI().toString())
                    .setActionFromIFrameSrcWhereTagContains("Main")
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final Matcher matcher1 = getMatcherAgainstContent("\\|(aHR0c[A-Za-z0-9\\+/]+)\\|");
            if (!matcher1.find()) {
                throw new PluginImplementationException("Error parsing page");
            }
            final String url = new String(Base64.decodeBase64(matcher1.group(1)), "UTF-8");
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

    @Override
    protected String getBaseURL() {
        return "http://share-links.biz";
    }

}
