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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

    private boolean isSingleLinkURL() {
        return PlugUtils.find("/\\?[A-Z0-9]{2}=", fileURL);
    }

    private void setConfig() throws Exception {
        EmbedUploadServiceImpl service = (EmbedUploadServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (isSingleLinkURL()) return;
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
            httpFile.setFileName(matcher.group(1).trim());
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2).trim()));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final List<URI> list = new LinkedList<URI>();
        final boolean isSingleLinkURL = isSingleLinkURL();
        if (isSingleLinkURL) {
            processLink(fileURL, list);
        } else {
            final GetMethod method = getGetMethod(fileURL);
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            checkNameAndSize();
            final Matcher matcher = getMatcherAgainstContent("<a href=\"(http://www\\.embedupload\\.com/\\?[A-Z0-9]{2}=[A-Z0-9]+?)\"");
            while (matcher.find()) {
                try {
                    processLink(matcher.group(1), list);
                } catch (ErrorDuringDownloadingException e) {
                    // discard the exception, sometimes they throw "You should click on the download link : not authorized" error message
                }
            }
        }
        if (list.isEmpty()) throw new PluginImplementationException("No links found");
        setConfig();
        if (isSingleLinkURL || config.isQueueAllLinks()) { //queue all links 
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

    private void processLink(final String singleLinkPage, final List<URI> list) throws ErrorDuringDownloadingException, IOException {
        final HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(singleLinkPage)
                .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        final Matcher matcher = getMatcherAgainstContent("You should.+?(https?://.+?)[\\s'\"]");
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing link");
        }
        try {
            list.add(new URI(matcher.group(1)));
        } catch (final URISyntaxException e) {
            LogUtils.processException(logger, e);
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Invalid file name")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}