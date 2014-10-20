package cz.vity.freerapid.plugins.services.canalbrasilglobo;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.JsonMapper;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u4
 */
class CanalBrasilGloboFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CanalBrasilGloboFileRunner.class.getName());
    private final Random random = new Random();
    private SettingsConfig config;


    private void setConfig() throws Exception {
        CanalBrasilGloboServiceImpl service = (CanalBrasilGloboServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(getPlaylistUrl(getVideoId(fileURL)));
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getPlaylistRootNode(getContentAsString()));
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(JsonNode playlistRootNode) throws ErrorDuringDownloadingException {
        String title = playlistRootNode.findPath("title").getTextValue();
        if (title == null) {
            throw new PluginImplementationException("Title not found");
        }
        String program = playlistRootNode.findPath("program").getTextValue();
        String filename = (program == null ? title : program + " - " + title) + ".mp4";
        httpFile.setFileName(filename);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        String videoId = getVideoId(fileURL);
        final GetMethod method = getGetMethod(getPlaylistUrl(videoId));
        if (makeRedirectedRequest(method)) {
            checkProblems();
            JsonNode playlistRootNode = getPlaylistRootNode(getContentAsString());
            checkNameAndSize(playlistRootNode);

            setConfig();
            CanalBrasilGloboVideo selectedVideo = getSelectedVideo(playlistRootNode);
            logger.info("Config settings : " + config);
            logger.info("Selected video  : " + selectedVideo);
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(String.format("http://security.video.globo.com/videos/%s/hash", videoId))
                    .setParameter("player", "flash")
                    .setParameter("version", "2.9.9.54")
                    .setParameter("resource_id", selectedVideo.resourceId)
                    .setParameter("_" + generateRandomString(3), String.valueOf(System.currentTimeMillis()))
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            logger.info(getContentAsString());
            String hash;
            try {
                hash = PlugUtils.getStringBetween(getContentAsString(), "\"hash\":\"", "\"");
            } catch (PluginImplementationException e) {
                throw new PluginImplementationException("Error getting 'hash'");
            }
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(selectedVideo.url)
                    .setParameter("h", new Crypto(hash).sign())
                    .setParameter("k", "flash")
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
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
        if (contentAsString.contains("página não encontrada")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String getVideoId(String fileUrl) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("/videos/(\\d+)\\.html$", fileUrl);
        if (!matcher.find()) {
            throw new PluginImplementationException("Video ID not found");
        }
        String videoId = matcher.group(1);
        logger.info("Video ID: " + videoId);
        return videoId;
    }

    private String getPlaylistUrl(String videoId) {
        return String.format("http://api.globovideos.com/videos/%s/playlist", videoId);
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) (97 + random.nextInt(25)));
        }
        return sb.toString();
    }

    private JsonNode getPlaylistRootNode(String playlistContent) throws PluginImplementationException {
        JsonNode rootNode;
        try {
            rootNode = new JsonMapper().getObjectMapper().readTree(playlistContent);
        } catch (IOException e) {
            throw new PluginImplementationException("Error getting playlist root node");
        }
        return rootNode;
    }

    private CanalBrasilGloboVideo getSelectedVideo(JsonNode playlistRootNode) throws PluginImplementationException {
        List<CanalBrasilGloboVideo> videoList = new LinkedList<CanalBrasilGloboVideo>();
        try {
            JsonNode resourcesNodes = playlistRootNode.findPath("resources");
            for (JsonNode resourcesNode : resourcesNodes) {
                JsonNode playersNodes = resourcesNode.get("players");
                if (playersNodes == null) { //skip non player resource, eg.thumbnail
                    continue;
                }
                boolean isFlash = false;
                for (JsonNode playersNode : playersNodes) {
                    if (playersNode.getTextValue().equalsIgnoreCase("flash")) {
                        isFlash = true;
                        break;
                    }
                }
                if (!isFlash) { //only support flash
                    continue;
                }
                String resourceId = resourcesNode.get("_id").getTextValue();
                int height = resourcesNode.get("height").getIntValue(); //height as quality
                String url = resourcesNode.get("url").getTextValue();
                CanalBrasilGloboVideo video = new CanalBrasilGloboVideo(resourceId, height, url);
                videoList.add(video);
                logger.info("Found: " + video);
            }
        } catch (Exception e) {
            throw new PluginImplementationException("Error parsing playlist");
        }
        if (videoList.isEmpty()) {
            throw new PluginImplementationException("No available video");
        }
        return Collections.min(videoList);
    }

    private class CanalBrasilGloboVideo implements Comparable<CanalBrasilGloboVideo> {
        private final static int LOWER_QUALITY_PENALTY = 10;
        private final String resourceId;
        private final int videoQuality;
        private final String url;
        private final int weight;

        public CanalBrasilGloboVideo(final String resourceId, final int videoQuality, final String url) {
            this.resourceId = resourceId;
            this.videoQuality = videoQuality;
            this.url = url;
            this.weight = calcWeight();
        }

        private int calcWeight() {
            VideoQuality configQuality = config.getVideoQuality();
            int deltaQ = videoQuality - configQuality.getQuality();
            return (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ);
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public int compareTo(final CanalBrasilGloboVideo that) {
            return Integer.valueOf(this.weight).compareTo(that.weight);
        }

        @Override
        public String toString() {
            return "CanalBrasilGloboVideo{" +
                    "resourceId='" + resourceId + '\'' +
                    ", videoQuality=" + videoQuality +
                    ", url='" + url + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }

}
