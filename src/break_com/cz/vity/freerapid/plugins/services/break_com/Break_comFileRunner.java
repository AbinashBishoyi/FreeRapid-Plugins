package cz.vity.freerapid.plugins.services.break_com;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.JsonMapper;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.JsonNode;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u4
 */
class Break_comFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Break_comFileRunner.class.getName());
    private SettingsConfig config;

    private void setConfig() throws Exception {
        Break_comServiceImpl service = (Break_comServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            isAtHomepage(getMethod);
            checkProblems();
            checkNameAndSize(getEmbedVarsRootNode(getEmbedContent(fileURL)));
        } else {
            isAtHomepage(getMethod);
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(JsonNode rootNode) throws ErrorDuringDownloadingException {
        String filename = rootNode.findPath("contentName").getTextValue();
        if (filename == null) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(filename.trim() + ".mp4");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            isAtHomepage(method);
            checkProblems();
            JsonNode embedVarsRootNode = getEmbedVarsRootNode(getEmbedContent(fileURL));
            checkNameAndSize(embedVarsRootNode);

            String youtubeId = embedVarsRootNode.get("youtubeId").getTextValue();
            if (youtubeId != null) {
                String youtubeUrl = "https://www.youtube.com/watch?v=" + youtubeId;
                logger.info("YouTube URL: " + youtubeUrl);
                httpFile.setNewURL(new URL(youtubeUrl));
                httpFile.setPluginID("");
                httpFile.setState(DownloadState.QUEUED);
            } else {
                setConfig();
                Break_comVideo selectedVideo = getSelectedVideo(embedVarsRootNode);
                logger.info("Config settings : " + config);
                logger.info("Selected video  : " + selectedVideo);
                final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(selectedVideo.url).toHttpMethod();
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            }
        } else {
            isAtHomepage(method);
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() {
        //
    }

    private void isAtHomepage(HttpMethod method) throws URIException, URLNotAvailableAnymoreException {
        if (method.getURI().toString().matches("http://(?:www\\.)break\\.com/?")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String getVideoId(String fileUrl) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("[^/]+?/.+?-(\\d+)$", fileUrl);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting video ID");
        }
        return matcher.group(1);
    }

    private String getEmbedContent(String fileUrl) throws Exception {
        if (!makeRedirectedRequest(getGetMethod("http://www.break.com/embed/" + getVideoId(fileUrl)))) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error requesting embed format");
        }
        checkProblems();
        return getContentAsString();
    }

    private JsonNode getEmbedVarsRootNode(String content) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("(?s)<script>\\s*?var embedVars\\s*?=\\s*?(\\{\\s*?.+?\\})\\s*?</script>", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting 'embedVars' content");
        }
        String embedVarsContent = matcher.group(1);
        JsonNode rootNode;
        try {
            rootNode = new JsonMapper().getObjectMapper().readTree(embedVarsContent);
        } catch (Exception e) {
            throw new PluginImplementationException("Error getting 'embedVars' root node");
        }
        return rootNode;
    }

    private Break_comVideo getSelectedVideo(JsonNode rootNode) throws PluginImplementationException {
        String authToken = rootNode.findPath("AuthToken").getTextValue();
        if (authToken == null) {
            throw new PluginImplementationException("Error getting auth token");
        }
        JsonNode mediaNodes = rootNode.get("media");
        if (mediaNodes == null) {
            throw new PluginImplementationException("Error getting 'media' nodes");
        }

        List<Break_comVideo> videoList = new LinkedList<Break_comVideo>();
        logger.info("Available videos :");
        for (JsonNode mediaNode : mediaNodes) {
            int quality;
            String url;
            try {
                quality = mediaNode.get("height").getIntValue();
                url = mediaNode.get("uri").getTextValue() + "?" + authToken;
            } catch (Exception e) {
                throw new PluginImplementationException("Error parsing 'media' nodes");
            }
            Break_comVideo video = new Break_comVideo(quality, url);
            videoList.add(video);
            logger.info(video.toString());
        }
        if (videoList.isEmpty()) {
            throw new PluginImplementationException("No available video");
        }
        return Collections.min(videoList);
    }

    private class Break_comVideo implements Comparable<Break_comVideo> {
        private final static int LOWER_QUALITY_PENALTY = 10;
        private final int videoQuality;
        private final String url;
        private final int weight;

        public Break_comVideo(final int videoQuality, final String url) {
            this.videoQuality = videoQuality;
            this.url = url;
            this.weight = calcWeight();
        }

        private int calcWeight() {
            VideoQuality configQuality = config.getVideoQuality();
            int deltaQ = videoQuality - configQuality.getQuality();
            return (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ); //prefer nearest better if the same quality doesn't exist
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public int compareTo(final Break_comVideo that) {
            return Integer.valueOf(this.weight).compareTo(that.weight);
        }

        @Override
        public String toString() {
            return "Break_comVideo{" +
                    "videoQuality=" + videoQuality +
                    ", url='" + url + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }

}
