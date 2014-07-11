package cz.vity.freerapid.plugins.services.apunkabollywood;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
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
 */
class ApunKaBollywoodFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ApunKaBollywoodFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        fixUrl();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            if (fileURL.contains("/browser/download/get/")) {
                checkNameAndSize();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void fixUrl() {
        if (fileURL.contains("/in/category/view/"))
            fileURL = fileURL.replaceFirst("/in/category/view/", "/browser/category/view/");
        if (fileURL.contains("/in/download/get/"))
            fileURL = fileURL.replaceFirst("/in/download/get/", "/browser/download/get/");
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String regexRule = "<a href=['\"]http://.+?/([^/]+)['\"].*?>.*?Click Here To Download.*?</a>";
        final Matcher matcher = getMatcherAgainstContent(regexRule);
        if (matcher.find()) {
            httpFile.setFileName(matcher.group(1).trim());
        } else {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        fixUrl();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (fileURL.contains("/browser/category/view/")) {
                processAlbum();
            } else if (fileURL.contains("/browser/download/get/")) {
                HttpMethod httpMethod;
                if (getContentAsString().contains("Free Download No More Available")) {
                    final String id = PlugUtils.getStringBetween(fileURL, "/download/get/", "/");
                    final HttpMethod aMethod = getMethodBuilder()
                            .setReferer(fileURL).setAjax()
                            .setAction("http://www.apunkabollywood.us/player/bridge/download/list/ids/" + id)
                            .toGetMethod();
                    if (!makeRedirectedRequest(aMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    final String fUrl = getContentAsString().split("\\|")[0];
                    httpMethod = getMethodBuilder()
                            .setReferer(fileURL)
                            .setAction(fUrl)
                            .toGetMethod();
                    httpFile.setFileName(fUrl.substring(fUrl.lastIndexOf("/") + 1));
                } else {
                    checkNameAndSize();
                    httpMethod = getMethodBuilder()
                            .setReferer(fileURL)
                            .setActionFromAHrefWhereATagContains("Click Here To Download")
                            .toGetMethod();
                }
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                throw new PluginImplementationException("Can't recognize URL");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Page not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void processAlbum() throws PluginImplementationException, URIException, URISyntaxException {
        final String urlListRegex = "<a href=['\"](http://.+?/download/get/.+?)['\"]>.+?</a><small>";
        final Matcher urlListMatcher = getMatcherAgainstContent(urlListRegex);
        final List<URI> uriList = new LinkedList<URI>();
        while (urlListMatcher.find()) {
            uriList.add(new java.net.URI(new org.apache.commons.httpclient.URI(urlListMatcher.group(1), false, "UTF-8").toString()));
        }
        if (uriList.isEmpty())
            throw new PluginImplementationException("No links found");
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        httpFile.getProperties().put("removeCompleted", true);
    }

}