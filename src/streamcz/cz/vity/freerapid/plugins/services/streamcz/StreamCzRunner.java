package cz.vity.freerapid.plugins.services.streamcz;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek
 * @author Ludek Zika
 * @author ntoskrnl
 * @author tong2shot
 */
class StreamCzRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(StreamCzRunner.class.getName());

    private SettingsConfig config;

    private void setConfig() throws Exception {
        StreamCzServiceImpl service = (StreamCzServiceImpl) getPluginService();
        config = service.getConfig();
    }

    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(String.format("http://www.stream.cz/ajax/get_video_source?context=catalogue&id=%s&%s", getId(), String.valueOf(Math.random())))
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            setConfig();
            StreamCzVideo streamCzVideo = getSelectedVideo(getContentAsString());
            logger.info("Config settings : " + config);
            logger.info("Downloading video : " + streamCzVideo);
            method = getGetMethod(streamCzVideo.url);
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getId() throws ErrorDuringDownloadingException {
        Matcher matcher = PlugUtils.matcher("[^/]+/(\\d+)-[^/]+", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting video ID");
        }
        return matcher.group(1);
    }

    private void checkName() throws Exception {
        final String name = PlugUtils.getStringBetween(getContentAsString(), "<meta property=\"og:title\" content=\"", "\"");
        httpFile.setFileName(name + ".mp4");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Stránku nebylo možné nalézt")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    StreamCzVideo getSelectedVideo(String content) throws Exception {
        List<StreamCzVideo> streamCzVideos = new LinkedList<StreamCzVideo>();
        Matcher matcher = PlugUtils.matcher("\"source\": \"(.+?)\", \"type\": \"(.+?)\".+?\"quality\": \"(\\d+)p\"", content);
        while (matcher.find()) {
            StreamCzVideo streamCzVideo = new StreamCzVideo(Integer.parseInt(matcher.group(3)), matcher.group(2), matcher.group(1));
            streamCzVideos.add(streamCzVideo);
        }
        if (streamCzVideos.isEmpty()) {
            throw new PluginImplementationException("No available videos");
        }
        return Collections.min(streamCzVideos);
    }

    private class StreamCzVideo implements Comparable<StreamCzVideo> {
        private final static int LOWER_QUALITY_PENALTY = 10;
        private final static int NON_MP4_PENALTY = 1;
        private final int videoQuality;
        private final String videoType;
        private final String url;
        private final int weight;

        private StreamCzVideo(int videoQuality, String videoType, String url) {
            this.videoQuality = videoQuality;
            this.videoType = videoType;
            this.url = url;
            this.weight = calcWeight();
            logger.info("Found video : " + this);
        }

        private int calcWeight() {
            VideoQuality configQuality = config.getVideoQuality();
            int deltaQ = videoQuality - configQuality.getQuality();
            int tempWeight = (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ);
            if (!videoType.contains("mp4")) {
                tempWeight += NON_MP4_PENALTY;
            }
            return tempWeight;
        }

        @Override
        public int compareTo(StreamCzVideo that) {
            return Integer.valueOf(this.weight).compareTo(that.weight);
        }

        @Override
        public String toString() {
            return "StreamCzVideo{" +
                    "videoQuality=" + videoQuality +
                    ", videoType='" + videoType + '\'' +
                    ", url='" + url + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }

}