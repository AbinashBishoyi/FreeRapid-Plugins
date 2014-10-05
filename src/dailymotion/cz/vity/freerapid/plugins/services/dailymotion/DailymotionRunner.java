package cz.vity.freerapid.plugins.services.dailymotion;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.JsonMapper;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Ramsestom, JPEXS, ntoskrnl, tong2shot
 */
class DailymotionRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DailymotionRunner.class.getName());
    private final static String DEFAULT_FILE_EXT = ".mp4";
    private DailymotionSettingsConfig config;

    private static enum Container {FLV, MP4}

    private void setConfig() throws Exception {
        DailymotionServiceImpl service = (DailymotionServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private void checkURL() {
        fileURL = fileURL.replace("/embed/video/", "/video/")
                .replace("/swf/video/", "/video/")
                .replace("/swf/", "/video/");
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".dailymotion.com", "lang", "en_EN", "/", 86400, false));
        setFileStreamContentTypes(new String[0], new String[]{"application/json"});
        if (!(isPlaylist() || isGroup())) {
            checkURL();
            checkName();
        }
    }

    //reference : http://www.dailymotion.com/doc/api/obj-video.html
    private void checkName() throws Exception {
        final HttpMethod method = getMethodBuilder()
                .setReferer(null)
                .setAction("https://api.dailymotion.com/video/" + getVideoIdFromURL())
                .setParameter("family_filter", "false")
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();

        //Sometimes file name contains double-quote sign, which is tricky to parse.
        //So JsonMapper is used to detect file name
        Map deserialized;
        String fname = null;
        try {
            JsonMapper jsonMapper = new JsonMapper();
            deserialized = jsonMapper.deserialize(getContentAsString(), Map.class);
            fname = PlugUtils.unescapeUnicode(deserialized.get("title") + DEFAULT_FILE_EXT);
        } catch (Exception e) {
            //
        }
        if (fname == null) {
            throw new PluginImplementationException("Error getting file name");
        }
        logger.info("File name: " + fname);
        httpFile.setFileName(fname);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".dailymotion.com", "lang", "en_EN", "/", 86400, false));
        setFileStreamContentTypes(new String[0], new String[]{"application/json"});
        if (isPlaylist()) {
            parsePlaylist();
        } else if (isGroup()) {
            parseGroup();
        } else {
            downloadVideo();
        }
    }

    private void downloadVideo() throws Exception {
        checkURL();
        checkName();
        setConfig();
        if (config.isSubtitleDownload()) {
            downloadSubtitles();
        }
        //They block some videos in some countries.
        //Request embed format to avoid blocking.
        final String embedUrl = String.format("http://www.dailymotion.com/embed/video/%s", getVideoIdFromURL());
        HttpMethod method = getGetMethod(embedUrl);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            List<DailyMotionVideo> dailyMotionVideos = getDailyMotionVideosFromSequence(getContentAsString(), Container.MP4);
            final DailyMotionVideo dmv = getSelectedDailyMotionVideo(dailyMotionVideos);
            logger.info("Quality setting : " + config.getVideoQuality());
            logger.info("Video to be downloaded : " + dmv);
            String url = dmv.url;
            method = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
            setClientParameter(DownloadClientConsts.IGNORE_ACCEPT_RANGES, true);
            httpFile.setResumeSupported(true);
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException("Error getting embed URL");
        }
    }

    private List<DailyMotionVideo> getDailyMotionVideosFromSequence(String sequence, Container container) {
        final List<DailyMotionVideo> dailyMotionVideos = new LinkedList<DailyMotionVideo>();
        for (VideoQuality videoQuality : VideoQuality.getItems()) {
            final String urlRegex = String.format("\"stream_h264_?(?:%s)_url\":\"(.+?)\"", videoQuality.getQualityToken1() + "|" + videoQuality.getQualityToken2());
            final Matcher matcher = PlugUtils.matcher(urlRegex, sequence);
            if (matcher.find()) {
                final String url = matcher.group(1).replace("\\/", "/");
                final DailyMotionVideo dmv = new DailyMotionVideo(videoQuality, container, url);
                dailyMotionVideos.add(dmv);
            }
        }
        return dailyMotionVideos;
    }

    private DailyMotionVideo getSelectedDailyMotionVideo(List<DailyMotionVideo> dailyMotionVideos) throws PluginImplementationException {
        logger.info("Available videos :");
        for (DailyMotionVideo dmv : dailyMotionVideos) {
            logger.info(dmv.toString());
        }
        if (dailyMotionVideos.isEmpty()) {
            throw new PluginImplementationException("No available video");
        }
        return Collections.min(dailyMotionVideos);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("\"message\":\"Can not find the object")
                || contentAsString.contains("\"message\":\"This video has been censored.\"")
                || contentAsString.contains("\"message\":\"This video does not exist or has been deleted.\"")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private MethodBuilder getDailyMotionMethodBuilder(String action, int page, String fields) throws BuildMethodException {
        return getMethodBuilder()
                .setReferer(null)
                .setAction(action)
                .setParameter("page", String.valueOf(page))
                .setParameter("limit", "100")
                .setParameter("family_filter", "false")
                .setParameter("fields", fields);
    }

    //reference : http://www.dailymotion.com/doc/api/obj-video.html#obj-video-cnx-subtitles
    private void downloadSubtitles() throws Exception {
        final String videoId = getVideoIdFromURL();
        final String action = String.format("https://api.dailymotion.com/video/%s/subtitles", videoId);
        int page = 1;
        do {
            if (!makeRedirectedRequest(getDailyMotionMethodBuilder(action, page++, "language,url").toGetMethod())) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final Matcher matcher = getMatcherAgainstContent("\"language\":\"(.+?)\",\"url\":\"(.+?)\"");
            while (matcher.find()) {
                String lang = matcher.group(1).trim();
                String subtitleUrl = matcher.group(2).trim().replace("\\/", "/");
                if (!lang.isEmpty() && !subtitleUrl.isEmpty()) {
                    SubtitleDownloader subtitleDownloader = new SubtitleDownloader();
                    try {
                        subtitleDownloader.downloadSubtitle(client, httpFile, subtitleUrl, lang);
                    } catch (Exception e) {
                        LogUtils.processException(logger, e);
                    }
                }
            }
        } while (getContentAsString().contains("\"has_more\":true"));
    }

    //reference : http://www.dailymotion.com/doc/api/advanced-api.html#response
    private LinkedList<URI> getURIList(final String action) throws Exception {
        final LinkedList<URI> uriList = new LinkedList<URI>();
        int page = 1;
        do {
            if (!makeRedirectedRequest(getDailyMotionMethodBuilder(action, page++, "url").toGetMethod())) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final Matcher matcher = getMatcherAgainstContent("\"url\":\"(.+?)\"");
            while (matcher.find()) {
                try {
                    uriList.add(new URI(matcher.group(1).replace("\\/", "/")));
                } catch (final URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
        } while (getContentAsString().contains("\"has_more\":true"));
        return uriList;
    }

    private void queueLinks(final List<URI> uriList) throws PluginImplementationException {
        if (uriList.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        httpFile.getProperties().put("removeCompleted", true);
        logger.info(uriList.size() + " videos added");
    }

    private String getVideoIdFromURL() throws PluginImplementationException {
        final Matcher matcher = PlugUtils.matcher("/video/([^_#/?]+)", fileURL);
        if (!matcher.find()) throw new PluginImplementationException("Unable to get video id");
        return matcher.group(1);
    }

    private boolean isPlaylist() {
        return fileURL.contains("/playlist/");
    }

    private String getPlaylistIdFromURL() throws PluginImplementationException {
        final Matcher matcher = PlugUtils.matcher("/playlist/([^_#/]+)", fileURL);
        if (!matcher.find()) throw new PluginImplementationException("Unable to get playlist id");
        return matcher.group(1);
    }

    //reference : http://www.dailymotion.com/doc/api/obj-playlist.html
    //reference : http://www.dailymotion.com/doc/api/explorer#/playlist/videos/list
    private void parsePlaylist() throws Exception {
        final String playlistId = getPlaylistIdFromURL();
        final String action = String.format("https://api.dailymotion.com/playlist/%s/videos", playlistId);
        final List<URI> uriList = getURIList(action);
        queueLinks(uriList);
    }

    private boolean isGroup() {
        return fileURL.contains("/group/");
    }

    private String getGroupIdFromURL() throws PluginImplementationException {
        final Matcher matcher = PlugUtils.matcher("/group/([^#/]+)", fileURL);
        if (!matcher.find()) throw new PluginImplementationException("Unable to get group id");
        return matcher.group(1);
    }

    //reference : http://www.dailymotion.com/doc/api/obj-group.html
    private void parseGroup() throws Exception {
        final String groupId = getGroupIdFromURL();
        final String action = String.format("https://api.dailymotion.com/group/%s/videos", groupId);
        final List<URI> uriList = getURIList(action);
        queueLinks(uriList);
    }

    private class DailyMotionVideo implements Comparable<DailyMotionVideo> {
        private final static int NEAREST_LOWER_PENALTY = 10;
        private final static int FLV_PENALTY = 1;
        private final VideoQuality videoQuality;
        private final Container container;
        private final String url;
        private final int weight;

        private DailyMotionVideo(final VideoQuality videoQuality, final Container container, final String url) {
            this.videoQuality = videoQuality;
            this.container = container;
            this.url = url;
            this.weight = calcWeight();
        }

        private int calcWeight() {
            final VideoQuality configQuality = config.getVideoQuality();
            final int deltaQ = videoQuality.getQuality() - configQuality.getQuality();
            int weight = (deltaQ < 0 ? Math.abs(deltaQ) + NEAREST_LOWER_PENALTY : deltaQ); //prefer nearest better if the same quality doesn't exist
            if (container == Container.FLV) { //prefer MP4 on same quality
                weight += FLV_PENALTY;
            }
            return weight;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public int compareTo(DailyMotionVideo that) {
            return Integer.valueOf(this.weight).compareTo(that.weight);
        }

        @Override
        public String toString() {
            return "DailyMotionVideo{" +
                    "videoQuality=" + videoQuality +
                    ", container='" + container + '\'' +
                    ", url='" + url + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }

}
