package cz.vity.freerapid.plugins.services.liveleak;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u3
 */
class LiveLeakFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LiveLeakFileRunner.class.getName());

    private SettingsConfig config;

    private void setConfig() throws Exception {
        LiveLeakServiceImpl service = (LiveLeakServiceImpl) getPluginService();
        config = service.getConfig();
    }

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
        Matcher matcher = PlugUtils.matcher("<span class=\"section_title\"[^<>]*?>(.+?)<", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        String filename = matcher.group(1).replace("&nbsp;", " ").trim() + ".mp4";
        logger.info("File name : " + filename);
        httpFile.setFileName(filename);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            setConfig();
            LiveLeakVideo liveLeakVideo = getSelectedLiveLeakVideo(getContentAsString());
            logger.info("Config settings :" + config);
            logger.info("Downloading video : " + liveLeakVideo);
            setFileStreamContentTypes("text/plain");
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(liveLeakVideo.url).toHttpMethod();
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
        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private LiveLeakVideo getSelectedLiveLeakVideo(String content) throws Exception {
        List<LiveLeakVideo> liveLeakVideos = new LinkedList<LiveLeakVideo>();
        String videoUrl;
        if (content.contains("hd_file_url")) {
            videoUrl = URLDecoder.decode(PlugUtils.getStringBetween(content, "hd_file_url=", "&"), "UTF-8");
            liveLeakVideos.add(new LiveLeakVideo(VideoQuality.HD, videoUrl));
        }
        if (content.contains("file_url")) {
            videoUrl = URLDecoder.decode(PlugUtils.getStringBetween(content, "file_url=", "&"), "UTF-8");
            liveLeakVideos.add(new LiveLeakVideo(VideoQuality.SD, videoUrl));
        }
        if (liveLeakVideos.isEmpty()) {
            throw new PluginImplementationException("No available videos");
        }
        return Collections.min(liveLeakVideos);
    }

    private class LiveLeakVideo implements Comparable<LiveLeakVideo> {
        private final static int LOWER_PENALTY_PENALTY = 10;
        private final VideoQuality videoQuality;
        private final String url;
        private final int weight;

        private LiveLeakVideo(VideoQuality videoQuality, String url) {
            this.videoQuality = videoQuality;
            this.url = url;
            this.weight = calcWeight();
            logger.info("Found video : " + this);
        }

        private int calcWeight() {
            VideoQuality configQuality = config.getVideoQuality();
            int deltaQ = videoQuality.getQuality() - configQuality.getQuality();
            return (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_PENALTY_PENALTY : deltaQ);
        }

        @Override
        public int compareTo(LiveLeakVideo that) {
            return Integer.valueOf(this.weight).compareTo(that.weight);
        }

        @Override
        public String toString() {
            return "LiveLeakVideo{" +
                    "videoQuality=" + videoQuality +
                    ", url='" + url + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }
}
