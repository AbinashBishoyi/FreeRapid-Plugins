package cz.vity.freerapid.plugins.services.directmirror;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.URIUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u2
 */
class DirectMirrorFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DirectMirrorFileRunner.class.getName());

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
        Matcher matcher = Pattern.compile("File Name :.+?<b>(.+?)</b>", Pattern.DOTALL).matcher(content);
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(matcher.group(1).trim());

        matcher = Pattern.compile("File Size :.+?<b>(.+?)</b>", Pattern.DOTALL).matcher(content);
        if (!matcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1).trim()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private DirectMirrorSettingsConfig getConfig() throws Exception {
        final DirectMirrorServiceImpl service = (DirectMirrorServiceImpl) getPluginService();
        return service.getConfig();
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(fileURL.replace("/files/", "/status.php?uid="))
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final List<URI> list = getMirrors();
            if (list.isEmpty()) {
                throw new URLNotAvailableAnymoreException("No available mirrors");
            }
            final DirectMirrorSettingsConfig config = getConfig();
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

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file was removed")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private List<URI> getMirrors() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("<a href=(.+?) target");
        final List<URI> list = new LinkedList<URI>();
        while (matcher.find()) {
            final HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(matcher.group(1))
                    .toGetMethod();
            final int httpStatus = client.makeRequest(method, false);
            final String url;
            if (httpStatus / 100 == 3) {
                final Header locationHeader = method.getResponseHeader("Location");
                if (locationHeader == null) {
                    throw new PluginImplementationException("Invalid redirect");
                }
                url = locationHeader.getValue();
            } else if (httpStatus == 200) {
                url = PlugUtils.getStringBetween(getContentAsString(), "\"main\" src=\"", "\"");
            } else { // != 200
                throw new ServiceConnectionProblemException();
            }
            try {
                list.add(new URI(URIUtil.encodePathQuery(url, "UTF-8")));
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        return list;
    }

}