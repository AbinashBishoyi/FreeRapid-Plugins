package cz.vity.freerapid.plugins.services.vk;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u2
 */
class VkFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(VkFileRunner.class.getName());
    private VkSettingsConfig config;

    private void setConfig() throws Exception {
        VkServiceImpl service = (VkServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (!isEmbeddedUrl()) {
            fileURL = getEmbeddedUrl(fileURL);
        }
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException, UnsupportedEncodingException {
        try {
            String fn = PlugUtils.getStringBetween(content, "var video_title = '", "';").trim();
            httpFile.setFileName(URLDecoder.decode(fn, "UTF-8").trim() + ".mp4");
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("Filename not found");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        if (!isEmbeddedUrl()) {
            fileURL = getEmbeddedUrl(fileURL);
        }
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            setConfig();
            VkVideo vkVideo = getSelectedVkVideo();
            logger.info("Config quality : " + config.getVideoQuality());
            logger.info("Video to be downloaded : " + vkVideo);
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(vkVideo.url).toHttpMethod();
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
        if (contentAsString.contains("No videos found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private boolean isEmbeddedUrl() {
        return fileURL.contains("video_ext.php");
    }

    private String getEmbeddedUrl(String fileURL) throws Exception {
        logger.info("Getting embedded URL..");
        Matcher matcher = PlugUtils.matcher("video(-?\\d+)_(-?\\d+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Unknown URL pattern");
        }
        String userId = matcher.group(1);
        String videoId = matcher.group(2);
        //sometimes requires login, redirect to biqle as workaround
        //alternatives :
        //http://hdxit.ru/video/140538996_164236408/
        //http://mirhdtv.ru/video/-36880507_165363780/
        String biqleUrl = String.format("http://biqle.ru/watch/%s_%s", userId, videoId);
        GetMethod method = getGetMethod(biqleUrl);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error getting embedded URL");
        }
        matcher = getMatcherAgainstContent("src=\"(http://vk\\.com/video_ext\\.php.+?)\"");
        if (!matcher.find()) {
            throw new PluginImplementationException("Cannot find embedded URL");
        }
        return matcher.group(1);
    }

    private VkVideo getSelectedVkVideo() throws Exception {
        Matcher matcher = getMatcherAgainstContent("\"url(\\d{3})\":\"(http.+?)\"");
        List<VkVideo> vkVideos = new ArrayList<VkVideo>();
        logger.info("Available videos :");
        while (matcher.find()) {
            int quality = Integer.parseInt(matcher.group(1));
            String url = matcher.group(2).replace("\\/", "/");
            VkVideo vkVideo = new VkVideo(VideoQuality.valueOf("_" + quality), url);
            vkVideos.add(vkVideo);
            logger.info(vkVideo.toString());
        }
        if (vkVideos.isEmpty()) {
            throw new PluginImplementationException("Video quality list is empty");
        }
        return Collections.min(vkVideos);
    }

    private class VkVideo implements Comparable<VkVideo> {
        private final static int NEAREST_LOWER_PENALTY = 10;
        private final VideoQuality videoQuality;
        private final String url;
        private int weight;

        public VkVideo(final VideoQuality videoQuality, final String url) {
            this.videoQuality = videoQuality;
            this.url = url;
            calcWeight();
        }

        private void calcWeight() {
            VideoQuality configQuality = config.getVideoQuality();
            int deltaQ = videoQuality.getQuality() - configQuality.getQuality();
            weight = (deltaQ < 0 ? Math.abs(deltaQ) + NEAREST_LOWER_PENALTY : deltaQ); //prefer nearest better if the same quality doesn't exist
        }

        @Override
        public int compareTo(final VkVideo that) {
            return Integer.valueOf(this.weight).compareTo(that.weight);
        }

        @Override
        public String toString() {
            return "VkVideo{" +
                    "videoQuality=" + videoQuality +
                    ", url='" + url + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }

}