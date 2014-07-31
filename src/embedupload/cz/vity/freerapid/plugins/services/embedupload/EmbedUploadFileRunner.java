package cz.vity.freerapid.plugins.services.embedupload;

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
import org.apache.commons.httpclient.methods.GetMethod;

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
 */
class EmbedUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(EmbedUploadFileRunner.class.getName());
    private EmbedUploadSettingsConfig config;

    private void setConfig() throws Exception {
        EmbedUploadServiceImpl service = (EmbedUploadServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (isSingleLink(fileURL)) {
            return;
        }
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("class=\"form-title\".*?>\\s*(.+?)<br>\\s*<div class=\"grey-text\">\\s*\\((.+?)\\)\\s*<br>");
        if (!matcher.find()) {
            logger.warning("File name and/or file size not found");
        } else {
            httpFile.setFileName("Links: " + matcher.group(1).replace(" <br>", "").trim());
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2).trim()));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        if (isSingleLink(fileURL)) {
            processSingleLink(fileURL);
        } else {
            final GetMethod method = getGetMethod(fileURL);
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            checkNameAndSize();
            final List<URI> list = getMirrors(getContentAsString());
            if (list.isEmpty()) {
                throw new PluginImplementationException("No available mirrors");
            }
            setConfig();
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
        }
    }

    private boolean isSingleLink(String fileUrl) {
        return PlugUtils.find("/\\?[A-Z0-9]{2}=", fileUrl);
    }

    private String getTargetUrl(String fromUrl) throws Exception {
        HttpMethod method = getGetMethod(fromUrl);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        final Matcher matcher = getMatcherAgainstContent("(?s)link\\s*?:\\s*?<a href=['\"](https?://.+?)['\"]");
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting target URL from: " + fromUrl);
        }
        return matcher.group(1).trim();
    }

    private void processSingleLink(String fileUrl) throws Exception {
        String targetUrl = getTargetUrl(fileUrl);
        httpFile.setNewURL(new URL(targetUrl));
        httpFile.setPluginID("");
        httpFile.setState(DownloadState.QUEUED);
    }

    private List<URI> getMirrors(String content) {
        final Matcher matcher = PlugUtils.matcher("href=['\"](http://www\\.embedupload\\.\\w+?/\\?[A-Z0-9]{2}=[A-Z0-9]+?)['\"]", content);
        final List<URI> list = new LinkedList<URI>();
        while (matcher.find()) {
            try {
                final String targetUrl = getTargetUrl(matcher.group(1));
                list.add(new URI(targetUrl));
            } catch (final Exception e) {
                LogUtils.processException(logger, e);
            }
        }
        return list;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Invalid file name")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}