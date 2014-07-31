package cz.vity.freerapid.plugins.services.mirrorcreator;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @author ntoskrnl
 */
class MirrorCreatorFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MirrorCreatorFileRunner.class.getName());

    private MirrorCreatorSettingsConfig getConfig() throws Exception {
        MirrorCreatorServiceImpl service = (MirrorCreatorServiceImpl) getPluginService();
        return service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (isSingleLink(fileURL)) {
            return;
        }
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<div class=\"file\">\\s*<h3>(.+?)\\((.+?)\\)</h3>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name/size not found");
        }
        httpFile.setFileName("Links: " + matcher.group(1));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        if (isSingleLink(fileURL)) {
            processSingleLink(fileURL);
        } else {
            HttpMethod method = getGetMethod(fileURL);
            if (makeRedirectedRequest(method)) {
                checkProblems();
                checkNameAndSize();
                fileURL = method.getURI().toString();
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAjax()
                        .setActionFromTextBetween(".open(\"GET\", \"", "\"")
                        .toGetMethod();
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                final List<URI> list = getMirrors(getContentAsString());
                if (list.isEmpty()) {
                    throw new PluginImplementationException("No available mirrors");
                }
                final MirrorCreatorSettingsConfig config = getConfig();
                if (config.isQueueAllLinks()) { //queue all links
                    getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                } else { //queue link using priority
                    final List<URL> urlList = new LinkedList<URL>();
                    for (final URI uri : list) {
                        final URL url = new URL(uri.toURL().toString());
                        urlList.add(url);
                    }
                    getPluginService().getPluginContext().getQueueSupport().addLinkToQueueUsingPriority(httpFile, urlList);
                }
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private boolean isSingleLink(String fileUrl) {
        return PlugUtils.find("/showlink\\.php\\?uid=[A-Z0-9]+?&hostid=\\d+", fileUrl);
    }

    private String getTargetUrl(String fromUrl) throws Exception {
        HttpMethod method = getGetMethod(fromUrl);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        String targetUrl;
        try {
            targetUrl = PlugUtils.getStringBetween(getContentAsString(), "redirecturl\">", "</div>");
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("Error getting target URL from " + fromUrl);
        }
        return targetUrl;
    }

    private void processSingleLink(String fileUrl) throws Exception {
        String targetUrl = getTargetUrl(fileUrl);
        httpFile.setNewURL(new URL(targetUrl));
        httpFile.setPluginID("");
        httpFile.setState(DownloadState.QUEUED);
    }

    private List<URI> getMirrors(String content) {
        final Matcher matcher = PlugUtils.matcher("(<a\\b[^<>]+?>)", content);
        final List<URI> list = new LinkedList<URI>();
        while (matcher.find()) {
            final String a = matcher.group(1);
            if (a.contains("target=\"_blank\"")) {
                try {
                    final String fromUrl = getMethodBuilder(a + "</a>")
                            .setReferer(fileURL)
                            .setActionFromAHrefWhereATagContains("")
                            .getEscapedURI();
                    final String targetUrl = getTargetUrl(fromUrl);
                    list.add(new URI(targetUrl));
                } catch (final Exception e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
        return list;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Link disabled or is invalid")
                || contentAsString.contains("the link you have clicked is not available")
                || contentAsString.contains("Links Unavailable")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    @Override
    protected String getBaseURL() {
        return "http://www.mirrorcreator.com";
    }

}