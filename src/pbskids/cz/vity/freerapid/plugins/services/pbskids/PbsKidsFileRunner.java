package cz.vity.freerapid.plugins.services.pbskids;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.tunlr.Tunlr;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u2
 */
class PbsKidsFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(PbsKidsFileRunner.class.getName());
    private final static String GET_HOST_PROGRAM_URL = "http://pbskids.org/pbsk/video/api/getHostProgram/?program=%s";
    private final static String GET_VIDEOS_URL_SEASON = "http://pbskids.org/pbsk/video/api/getVideos/?type=Episode&status=available&encoding=&endindex=150&program=%s&return=type,airdate,tags,captions,images&orderby=-airdate&tags=cc-season%s&k=%s";
    private final static String GET_VIDEOS_URL = "http://pbskids.org/pbsk/video/api/getVideos/?status=available&encoding=&endindex=150&program=%s&return=type,airdate,tags,captions,images&orderby=-airdate&k=%s";
    private SettingsConfig config;

    private void setConfig() throws Exception {
        PbsKidsServiceImpl service = (PbsKidsServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(String.format(GET_HOST_PROGRAM_URL, fileURL.replaceFirst("\\?.+", "")).replace("#!", "&k=" + generateK() + "#!"))
                .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        String program = PlugUtils.getStringBetween(getContentAsString(), "\"defaultProgram\":\"", "\"");
        checkNameAndSize(program);
    }

    private void checkNameAndSize(String program) throws Exception {

        if (!hasGuid(fileURL)) {
            httpFile.setFileName("[All videos] " + program);
        } else {
            httpFile.setFileName(program);
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(String.format(GET_HOST_PROGRAM_URL, fileURL.replaceFirst("\\?.+", "")))
                .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        String program = PlugUtils.getStringBetween(getContentAsString(), "\"defaultProgram\":\"", "\"");
        checkNameAndSize(program);

        String season = getSeasonFromUrl(fileURL);
        httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction((hasSeason(fileURL) ? String.format(GET_VIDEOS_URL_SEASON, URLEncoder.encode(program, "UTF-8"), season, generateK())
                        : String.format(GET_VIDEOS_URL, URLEncoder.encode(program, "UTF-8"), generateK())))
                .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        String videosInfoContent = getContentAsString();
        setConfig();
        Map<String, VideoClip> videoClipMap = getVideoClipMap(videosInfoContent);  ////k=guid, v=video clip
        if (hasGuid(fileURL)) { //has guid, download video
            String guidFromUrl = getGuidFromUrl(fileURL);
            VideoClip videoClip = videoClipMap.get(guidFromUrl);
            if (videoClip == null) {
                throw new PluginImplementationException("No video which guid = " + guidFromUrl);
            }

            httpFile.setFileName(program + " - " + videoClip.title + ".flv");
            Stream stream = Collections.min(videoClip.getStreamList());
            logger.info("Available streams :");
            logger.info(videoClip.toString());
            logger.info("Config settings : " + config);
            logger.info("Selected stream : " + stream);
            RtmpSession session = getSession(stream);
            tryDownloadAndSaveFile(session);

        } else { //no guid, queue videos
            final List<URI> list = new LinkedList<URI>();
            for (Map.Entry<String, VideoClip> videoClipEntry : videoClipMap.entrySet()) {
                try {
                    list.add(new URI(fileURL + "?guid=" + videoClipEntry.getKey()));
                } catch (final URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
            if (list.isEmpty()) {
                throw new URLNotAvailableAnymoreException("No available video");
            }
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
            logger.info(list.size() + " videos added");
            httpFile.setState(DownloadState.COMPLETED);
            httpFile.getProperties().put("removeCompleted", true);
        }
    }

    private Map<String, VideoClip> getVideoClipMap(String videosInfoContent) throws ErrorDuringDownloadingException {
        Map<String, VideoClip> videoClipMap = new HashMap<String, VideoClip>(); //k=guid, v=video clip
        Matcher videoClipMatcher = PlugUtils.matcher("[\\{,](\"guid\":.+?)\\}(?:,\\{|\\]\\})", videosInfoContent);
        Matcher guidMatcher = PlugUtils.matcher("\"guid\":\"(.+?)\"", videosInfoContent);
        Matcher titleMatcher = PlugUtils.matcher("\"title\":\"(.+?)\"", videosInfoContent);
        Matcher flashMatcher = PlugUtils.matcher("\"flash\":\\{(.+?)\\}\\},", videosInfoContent);
        Matcher bitrateUrlMatcher = PlugUtils.matcher("\"bitrate\":(\\d+|null).+?\"url\":\"(.+?)\"", videosInfoContent);
        while (videoClipMatcher.find()) {
            int videoClipContentStart = videoClipMatcher.start(1);
            int videoClipContentEnd = videoClipMatcher.end(1);
            guidMatcher.region(videoClipContentStart, videoClipContentEnd);
            titleMatcher.region(videoClipContentStart, videoClipContentEnd);
            flashMatcher.region(videoClipContentStart, videoClipContentEnd);
            if (!guidMatcher.find()) {
                throw new PluginImplementationException("GUID not found");
            }
            String guid = guidMatcher.group(1);
            if (!titleMatcher.find()) {
                throw new PluginImplementationException("Title not found");
            }
            String title = titleMatcher.group(1);
            VideoClip videoClip = new VideoClip(guid, title);
            if (!flashMatcher.find()) {
                throw new PluginImplementationException("Flash video not found");
            }
            bitrateUrlMatcher.region(flashMatcher.start(1), flashMatcher.end(1));
            while (bitrateUrlMatcher.find()) {
                String bitrate = bitrateUrlMatcher.group(1);
                try {
                    VideoQuality videoQuality = (bitrate.equals("null") ? VideoQuality._800 : VideoQuality.valueOf("_" + bitrate));
                    String url = bitrateUrlMatcher.group(2).replace("\\/", "/");
                    videoClip.addStream(new Stream(videoQuality, url));
                } catch (Exception e) {
                    throw new PluginImplementationException("Unknown bitrate : " + bitrate);
                }
            }

            videoClipMap.put(guid, videoClip);
        }
        return videoClipMap;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private boolean hasGuid(String fileURL) {
        return fileURL.contains("?guid=");
    }

    private String getGuidFromUrl(String fileURL) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("\\?guid=([a-z0-9\\-]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Unknown GUID pattern");
        }
        return matcher.group(1);
    }

    private boolean hasSeason(String fileURL) {
        return fileURL.contains("/seasons-");
    }

    private String getSeasonFromUrl(String fileURL) {
        Matcher matcher = PlugUtils.matcher("/seasons[^/]+?/(\\d+)(?:\\?|$)", fileURL);
        if (!matcher.find()) {
            return "1"; //default to 1
        }
        return matcher.group(1);
    }

    private String generateK() {
        StringBuilder sb = new StringBuilder(64);
        Random random = new Random();
        for (int i = 0; i < 64; i++) {
            sb.append(Integer.toHexString(random.nextInt(15) + 1));
        }
        return sb.toString();
    }

    private RtmpSession getSession(Stream stream) throws Exception {
        GetMethod getMethod = getGetMethod(stream.url + "?format=json");
        if (!client.getSettings().isProxySet()) {
            Tunlr.setupMethod(getMethod);
        }
        if (!makeRedirectedRequest(getMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        String videoURL = PlugUtils.getStringBetween(getContentAsString(), "\"url\": \"", "\"");
        logger.info("Video URL : " + videoURL);
        Matcher matcher = PlugUtils.matcher("://([^/]+)/(.+?)/(mp4:.+)", videoURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing stream URL");
        }
        String host = matcher.group(1);
        String app = matcher.group(2);
        String play = matcher.group(3);
        return new RtmpSession(host, 1935, app, play);
    }

    private class VideoClip {
        private final String guid;
        private final String title;
        private List<Stream> streamList = new ArrayList<Stream>();

        public VideoClip(String guid, String title) {
            this.guid = guid;
            this.title = title;
        }

        public void addStream(Stream stream) {
            streamList.add(stream);
        }

        public List<Stream> getStreamList() {
            return streamList;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Stream stream : streamList) {
                sb.append(stream);
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    private class Stream implements Comparable<Stream> {
        private final static int NEAREST_LOWER_PENALTY = 10;
        private final VideoQuality videoQuality;
        private final String url;
        private final int weight;

        public Stream(VideoQuality videoQuality, String url) throws ErrorDuringDownloadingException {
            this.videoQuality = videoQuality;
            this.url = url;
            this.weight = calcWeight();
        }

        private int calcWeight() {
            int weight;
            final VideoQuality configQuality = config.getVideoQuality();
            final int deltaQ = videoQuality.getQuality() - configQuality.getQuality();
            weight = (deltaQ < 0 ? Math.abs(deltaQ) + NEAREST_LOWER_PENALTY : deltaQ); //prefer nearest better if the same quality doesn't exist
            return weight;
        }

        @Override
        public int compareTo(Stream that) {
            return Integer.valueOf(this.weight).compareTo(that.weight);
        }

        @Override
        public String toString() {
            return "Stream{" +
                    "videoQuality=" + videoQuality +
                    ", url='" + url + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }

}