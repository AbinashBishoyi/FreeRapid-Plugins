package cz.vity.freerapid.plugins.services.myzukaru;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u2
 */
class MyZukaRuFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MyZukaRuFileRunner.class.getName());
    private final static String SERVICE_BASE_URL = "http://www.myzuka.ru";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        if (isSongUrl(fileURL)) {
            httpFile.setFileName(PlugUtils.getStringBetween(content, "<h1 class=\"blue\">", "</h1>").trim() + ".mp3");
            PlugUtils.checkFileSize(httpFile, content, "Размер:", "<br");
        } else if (isAlbum()) {
            PlugUtils.checkName(httpFile, content, "<h1 class=\"green\">", "</h1>");
        } else {
            throw new PluginImplementationException("Unknown URL pattern");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            if (isAlbum()) {
                parseAlbum();
            } else if (isSongUrl(fileURL)) {
                final HttpMethod httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromAHrefWhereATagContains("Скачать")
                        .toHttpMethod();
                setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                throw new PluginImplementationException("Unknown URL pattern");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Runtime Error")  //they don't provide proper "page not found"
                || contentAsString.contains("resource cannot be found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private boolean isSongUrl(final String url) {
        return url.matches("http://(?:www\\.)?myzuka\\.ru/Song/.+");
    }

    private boolean isAlbum() {
        return fileURL.contains("/Album/");
    }

    private void parseAlbum() throws Exception {
        final List<URI> uriList = new LinkedList<URI>();
        final Matcher urlMatcher = getMatcherAgainstContent("<a href=[\"'](.+?)[\"']>Скачать</a>");
        while (urlMatcher.find()) {
            final String url = SERVICE_BASE_URL + urlMatcher.group(1);
            if (!isSongUrl(url)) {
                throw new PluginImplementationException("Unrecognized song URL pattern"); //to prevent original link disappear when song url pattern unrecognized
            }
            try {
                uriList.add(new URI(url));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        if (uriList.isEmpty()) {
            throw new PluginImplementationException("No song URL found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
    }

}