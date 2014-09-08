package cz.vity.freerapid.plugins.services.videomeh;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.JsonMapper;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u4
 */
class VideoMehFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(VideoMehFileRunner.class.getName());
    private SettingsConfig config;

    private void setConfig() throws Exception {
        VideoMehServiceImpl service = (VideoMehServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        String videoId = getVideoId(fileURL);
        HttpMethod method = requestPlayerSettings(videoId);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getPlayerRootNode(getContentAsString()));
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(JsonNode playerRootNode) throws ErrorDuringDownloadingException {
        String filename;
        try {
            filename = playerRootNode.get("settings").get("video_details").get("video").get("title").getTextValue() + ".flv";
        } catch (Exception e) {
            throw new PluginImplementationException("Error getting video title");
        }
        httpFile.setFileName(filename);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        String videoId = getVideoId(fileURL);
        HttpMethod method = requestPlayerSettings(videoId);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            JsonNode playerRootNode = getPlayerRootNode(getContentAsString());
            checkNameAndSize(playerRootNode);

            setConfig();
            VideoMehVideo selectedVideo = getSelectedVideo(playerRootNode);
            String videoUrl = selectedVideo.url + (!selectedVideo.url.contains("?") ? "?" : "&") + new Crypto().parse(playerRootNode) + "start=0";
            if (!tryDownloadAndSaveFile(getGetMethod(videoUrl))) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The page or video you are looking for cannot be found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String getVideoId(String fileUrl) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("/video/([^/]+)", fileUrl);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting video ID");
        }
        return matcher.group(1);
    }

    private HttpMethod requestPlayerSettings(String videoId) throws IOException, ErrorDuringDownloadingException {
        HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(String.format("http://videomeh.com/player_control/settings.php?v=%s&em=TRUE&fv=v1.2.74&rv=1.0.1&rv=1.0.1", videoId))
                .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        return httpMethod;
    }

    private JsonNode getPlayerRootNode(String content) throws PluginImplementationException {
        ObjectMapper om = new JsonMapper().getObjectMapper();
        JsonNode playerRootNode;
        try {
            playerRootNode = om.readTree(content);
        } catch (Exception e) {
            throw new PluginImplementationException("Error parsing player's settings JSON (1)");
        }
        return playerRootNode;
    }

    private VideoMehVideo getSelectedVideo(JsonNode playerRootNode) throws PluginImplementationException {
        JsonNode resNodes = playerRootNode.get("settings").get("res");
        List<VideoMehVideo> videoList = new ArrayList<VideoMehVideo>();
        logger.info("Available videos :");
        try {
            for (JsonNode resNode : resNodes) {
                int quality = Integer.parseInt(resNode.get("l").getTextValue().replace("p", ""));
                String url = new String(Base64.decodeBase64(resNode.get("u").getTextValue())).trim();
                if (url.isEmpty()) {
                    continue;
                }
                VideoMehVideo video = new VideoMehVideo(quality, url);
                videoList.add(video);
                logger.info(video.toString());
            }
        } catch (Exception e) {
            throw new PluginImplementationException("Error getting video qualities and URLs");
        }
        if (videoList.isEmpty()) {
            throw new PluginImplementationException("No available videos");
        }
        VideoMehVideo selectedVideo = Collections.min(videoList);
        logger.info("Config settings : " + config);
        logger.info("Selected video  : " + selectedVideo);
        return selectedVideo;
    }

    private class VideoMehVideo implements Comparable<VideoMehVideo> {
        private final static int LOWER_QUALITY_PENALTY = 10;
        private final int videoQuality;
        private final String url;
        private int weight;

        public VideoMehVideo(final int videoQuality, final String url) {
            this.videoQuality = videoQuality;
            this.url = url;
            calcWeight();
        }

        private void calcWeight() {
            VideoQuality configQuality = config.getVideoQuality();
            int deltaQ = videoQuality - configQuality.getQuality();
            weight = (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ); //prefer nearest better if the same quality doesn't exist
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public int compareTo(final VideoMehVideo that) {
            return Integer.valueOf(this.weight).compareTo(that.weight);
        }

        @Override
        public String toString() {
            return "VideoMehVideo{" +
                    "videoQuality=" + videoQuality +
                    ", url='" + url + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }

}
