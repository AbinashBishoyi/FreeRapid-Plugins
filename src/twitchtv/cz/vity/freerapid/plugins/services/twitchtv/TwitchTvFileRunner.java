package cz.vity.freerapid.plugins.services.twitchtv;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.JsonMapper;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import jlibs.core.net.URLUtil;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class TwitchTvFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TwitchTvFileRunner.class.getName());
    private final static String TITLE_PARAM = "title";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (isVideoUrl(fileURL)) {
            return;
        }
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            isAtHomePage(getMethod);
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        String filename;
        try {
            filename = PlugUtils.getStringBetween(content, "content='", "' property='og:title'");
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("Filename not found");
        }
        httpFile.setFileName(PlugUtils.unescapeHtml(filename.trim()).replaceAll("\\s", " "));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        if (isVideoUrl(fileURL)) {
            processVideoUrl();
            return;
        }
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            isAtHomePage(method);
            checkProblems();
            checkNameAndSize(contentAsString);

            final String videoId;
            try {
                videoId = PlugUtils.getStringBetween(getContentAsString(), "\"videoId\":\"", "\"");
            } catch (PluginImplementationException e) {
                throw new PluginImplementationException("Video id not found");
            }
            final HttpMethod httpMethod = getGetMethod(String.format("https://api.twitch.tv/api/videos/%s?as3=t", videoId));
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            final String title = httpFile.getFileName();
            final List<URI> uriList = new LinkedList<URI>();
            JsonNode rootNode;
            try {
                rootNode = new JsonMapper().getObjectMapper().readTree(getContentAsString());
            } catch (IOException e) {
                throw new PluginImplementationException("Error getting video root node");
            }
            try {
                JsonNode chunksNode = rootNode.get("chunks");
                //for multi quality, pick non-live, as live sometimes cannot be downloaded.
                //only support 480p for non-live, can't find sample for other qualities.
                JsonNode urlParentNodes = (chunksNode.get("480p") != null ? chunksNode.get("480p") : chunksNode.get("live"));
                boolean multiPart = (urlParentNodes.size() > 1);
                int counter = 1;
                for (JsonNode urlNode : urlParentNodes) {
                    //http://media-cdn.twitch.tv/store44.media44/archives/2012-7-30/highlight_326746473.flv
                    String videoUrl = urlNode.get("url").getTextValue();
                    //add title (and counter for multipart) to url's tail so we can extract it later
                    final String url = getMethodBuilder()
                            .setAction(videoUrl)
                            .setAndEncodeParameter(TITLE_PARAM, title + (multiPart ? "_" + counter++ : ""))
                            .getEscapedURI();
                    uriList.add(new URI(url));
                }
            } catch (Exception e) {
                throw new PluginImplementationException("Error parsing JSON content");
            }
            if (uriList.isEmpty()) {
                throw new PluginImplementationException("No video link found");
            }
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            httpFile.setState(DownloadState.COMPLETED);
            httpFile.getProperties().put("removeCompleted", true);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("I'm sorry, that page is in another castle")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void isAtHomePage(final HttpMethod method) throws URLNotAvailableAnymoreException, URIException {
        if (method.getURI().toString().matches("http://(?:.+?\\.)?twitch\\.tv/.+?/videos/?")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private boolean isVideoUrl(final String url) {
        return url.matches("http://media-cdn\\.twitch\\.tv/[^/]+?/archives/.+?/.+?\\..{3}.*");
    }

    private void processVideoUrl() throws Exception {
        //http://media-cdn.twitch.tv/store48.media48/archives/2012-9-3/format_480p_330898023.flv -> original videoUrl
        //http://media-cdn.twitch.tv/store48.media48/archives/2012-9-3/format_480p_330898023.flv?title=Kings of Poverty for RAINN!-Mystery Tournament_2 -> title+"_"+counter added
        URL url = new URL(fileURL);
        String title = null;
        try {
            title = URLUtil.getQueryParams(fileURL, "UTF-8").get(TITLE_PARAM);
        } catch (Exception e) {
            //
        }
        fileURL = url.getProtocol() + "://" + url.getAuthority() + url.getPath();
        final String extension = fileURL.substring(fileURL.lastIndexOf("."));
        final String filename = (title == null ? PlugUtils.suggestFilename(fileURL) : URLDecoder.decode(title + extension, "UTF-8"));
        httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(filename, "_"));
        client.setReferer(httpFile.getFileUrl().getProtocol() + "://" + httpFile.getFileUrl().getAuthority());
        final GetMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

}