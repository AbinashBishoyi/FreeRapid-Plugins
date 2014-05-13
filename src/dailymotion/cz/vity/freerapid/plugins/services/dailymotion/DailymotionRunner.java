package cz.vity.freerapid.plugins.services.dailymotion;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.InflaterInputStream;

/**
 * Class which contains main code
 *
 * @author Ramsestom, JPEXS, ntoskrnl, tong2shot
 */
class DailymotionRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DailymotionRunner.class.getName());
    private final static String SUBTITLE_SEPARATOR = "?";
    private final static String DEFAULT_FILE_EXT = ".mp4";
    private DailymotionSettingsConfig config;

    private static enum UriListType {VIDEOS, SUBTITLES}

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
        addCookie(new Cookie(".dailymotion.com", "family_filter", "off", "/", 86400, false));
        addCookie(new Cookie(".dailymotion.com", "lang", "en_EN", "/", 86400, false));
        setFileStreamContentTypes(new String[0], new String[]{"application/json"});
        if (!(isPlaylist() || isGroup() || isSubtitle())) {
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
        PlugUtils.checkName(httpFile, getContentAsString(), "\"title\":\"", "\"");
        httpFile.setFileName(PlugUtils.unescapeUnicode(httpFile.getFileName()) + DEFAULT_FILE_EXT);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".dailymotion.com", "family_filter", "off", "/", 86400, false));
        addCookie(new Cookie(".dailymotion.com", "lang", "en_EN", "/", 86400, false));
        setFileStreamContentTypes(new String[0], new String[]{"application/json"});
        if (isPlaylist()) {
            parsePlaylist();
        } else if (isGroup()) {
            parseGroup();
        } else if (isSubtitle()) {
            downloadSubtitle();
        } else {
            downloadVideo();
        }
    }

    private void downloadVideo() throws Exception {
        checkURL();
        checkName();
        setConfig();
        if (config.isSubtitleDownload()) {
            queueSubtitles();
        }
        HttpMethod method = getGetMethod(fileURL);
        //dummy request
        if (makeRedirectedRequest(method)) {
            checkProblems();
            //they block some videos in some countries.
            //request swf to avoid blocking.
            final String swfUrl = String.format("http://www.dailymotion.com/swf/video/%s?autoPlay=1", getVideoIdFromURL());
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(swfUrl)
                    .toGetMethod();
            final InputStream is = client.makeRequestForFile(method);
            if (is == null) {
                throw new ServiceConnectionProblemException("Error downloading SWF");
            }
            final String swfStr = swfToString(is);
            final String sequence;
            List<DailyMotionVideo> dailyMotionVideos;
            if (swfStr.contains("ldURL")) { //mp4
                //find sequence in swf
                // \Q = begin quote, \E = end quote
                Matcher matcher = PlugUtils.matcher("(\\Q\\\"ldURL\\\":\\\"\\E.+?)(?:\\Q,\\\"cdn\\E|\\Q,\\\"autoURL\\E)", swfStr);
                if (!matcher.find()) {
                    throw new PluginImplementationException("Sequence not found in SWF");
                }
                sequence = matcher.group(1).replace("\\\"", "\"").replace("\\\\\\/", "/");
                logger.info("Sequence found in SWF");
                dailyMotionVideos = getDailyMotionVideosFromSequence(sequence, "MP4");
            } else if (swfStr.contains("autoURL")) { //flv
                //find manifest in swf
                Matcher matcher = PlugUtils.matcher("\\Q\\\"autoURL\\\":\\\"\\E(.+?)(?:\\Q\\\",\\\"cdn\\E|\\Q\\\",\\\"allowStageVideo\\E)", swfStr);
                if (!matcher.find()) {
                    throw new PluginImplementationException("Manifest not found in SWF");
                }
                final String manifestUrl = matcher.group(1).replace("\\\"", "\"").replace("\\\\\\/", "/");
                setTextContentTypes("application/vnd.lumberjack.manifest");
                method = getMethodBuilder()
                        .setReferer(swfUrl)
                        .setAction(manifestUrl)
                        .toGetMethod();
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                logger.info("Manifest found in SWF");
                logger.info("Manifest content : " + getContentAsString());
                //find sequence in manifest
                sequence = manifestToSequence(getContentAsString());
                logger.info("Sequence found in manifest");
                dailyMotionVideos = getDailyMotionVideosFromSequence(sequence, "FLV");

                if (swfStr.contains("video_url")) { //380p mp4, higher priority than 380p flv
                    //find 380p mp4 single quality sequence in swf
                    matcher = PlugUtils.matcher("\\Q\\\"video_url\\\":\\\"\\E(.+?)\\Q\\\",\\E", swfStr);
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Sequence (380p mp4) not found in SWF");
                    }
                    logger.info("Sequence (380p mp4) found in SWF");
                    dailyMotionVideos.add(new DailyMotionVideo(VideoQuality._380, "MP4", URLDecoder.decode(matcher.group(1).replace("\\\"", "\"").replace("\\\\\\/", "/"), "UTF-8")));
                }
            } else {
                throw new PluginImplementationException("External video channel is not supported");
            }

            final DailyMotionVideo dmv = getSelectedDailyMotionVideo(dailyMotionVideos);
            logger.info("Quality setting : " + config.getVideoQuality());
            logger.info("Video to be downloaded : " + dmv);
            String url = dmv.url;
            if (dmv.container.equalsIgnoreCase("FLV")) {
                method = getMethodBuilder()
                        .setReferer(swfUrl)
                        .setAction(url)
                        .toGetMethod();
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                //final String baseUrl = new URI(url).getAuthority();
                final String baseUrl = "vid2.ec.dmcdn.net";
                //get the original url, not the fragmented ones
                url = "http://" + baseUrl + PlugUtils.getStringBetween(getContentAsString(), "\"template\":\"", "\",").replace("frag($fragment$)/", "");
                httpFile.setFileName(httpFile.getFileName().replaceFirst(Pattern.quote(DEFAULT_FILE_EXT) + "$", ".flv"));
            }
            client.setReferer(fileURL);
            method = getGetMethod(urlDecode(url).replace("\\", ""));
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String manifestToSequence(final String manifest) {
        final Matcher matcher = PlugUtils.matcher("\"name\":\"(\\d+)\".+?\"template\":\"(.+?)\"", manifest);
        final StringBuilder sequenceSb = new StringBuilder();
        while (matcher.find()) {
            String qualityToken = matcher.group(1);
            for (VideoQuality videoQuality : VideoQuality.getItems()) {
                //replace 240 with "ldURL", and so on..
                qualityToken = qualityToken.replace(String.valueOf(videoQuality.getQuality()), "\"" + videoQuality.getQualityToken() + "\"");
            }
            sequenceSb.append(qualityToken);
            sequenceSb.append(":\"");
            sequenceSb.append(matcher.group(2));
            sequenceSb.append("\",");
        }
        return sequenceSb.toString();
    }

    private List<DailyMotionVideo> getDailyMotionVideosFromSequence(String sequence, String container) throws PluginImplementationException {
        final List<DailyMotionVideo> dailyMotionVideos = new LinkedList<DailyMotionVideo>();
        for (VideoQuality videoQuality : VideoQuality.getItems()) {
            final String qualityToken = videoQuality.getQualityToken();
            final String urlRegex = String.format("\"%s\":\"(.+?)\"", qualityToken);
            final Matcher matcher = PlugUtils.matcher(urlRegex, sequence);
            if (matcher.find()) {
                final String url = matcher.group(1);
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

    private String swfToString(InputStream is) throws Exception {
        try {
            final byte[] header = new byte[8];
            if (8 != is.read(header)) throw new IOException("Failed receiving SWF header");
            String strHeader = new String(header);
            if ((!strHeader.contains("FWS")) && (!strHeader.contains("CWS"))) {
                throw new IOException("Invalid SWF file");
            }
            if (strHeader.contains("CWS")) {
                is = new InflaterInputStream(is);
            }
            final byte[] buffer = new byte[1024];
            final StringBuilder sb = new StringBuilder(8192);
            int len;
            while ((len = is.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, len, "utf-8"));
            }
            return sb.toString();
        } finally {
            try {
                is.close();
            } catch (final Exception e) {
                LogUtils.processException(logger, e);
            }
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("\"message\":\"Can not find the object")
                || contentAsString.contains("\"message\":\"This video has been censored.\"")
                || contentAsString.contains("\"message\":\"This video does not exist or has been deleted.\"")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private static String urlDecode(final String url) {
        try {
            return url == null ? "" : URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LogUtils.processException(logger, e);
            return "";
        }
    }

    //reference : http://www.dailymotion.com/doc/api/advanced-api.html#response
    private LinkedList<URI> getURIList(final String action, UriListType uriListType) throws Exception {
        final LinkedList<URI> uriList = new LinkedList<URI>();
        int page = 1;
        do {
            final HttpMethod method = getMethodBuilder()
                    .setReferer(null)
                    .setAction(action)
                    .setParameter("page", String.valueOf(page++))
                    .setParameter("limit", "100")
                    .setParameter("family_filter", "false")
                    .setParameter("fields", uriListType == UriListType.VIDEOS ? "url" : "language,url")
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            if (uriListType == UriListType.VIDEOS) { //video
                final Matcher matcher = getMatcherAgainstContent("\"url\":\"(.+?)\"");
                while (matcher.find()) {
                    try {
                        uriList.add(new URI(matcher.group(1).replace("\\/", "/")));
                    } catch (final URISyntaxException e) {
                        LogUtils.processException(logger, e);
                    }
                }
            } else { //subtitle
                final Matcher matcher = getMatcherAgainstContent("\"language\":\"(.+?)\",\"url\":\"(.+?)\"");
                while (matcher.find()) {
                    try {
                        final String title = httpFile.getFileName().replace(DEFAULT_FILE_EXT, "");
                        final String language = matcher.group(1);
                        //add title and language to tail, so we can extract it later
                        //http://static2.dmcdn.net/static/video/339/362/35263933:subtitle_en.srt -> original subtitle url
                        //http://static2.dmcdn.net/static/video/339/362/35263933:subtitle_en.srt/test subtitle in dailymotion?en -> title and language added at tail
                        final String subtitleUrl = String.format(matcher.group(2).replace("\\/", "/") + "/%s%s%s", title, SUBTITLE_SEPARATOR, language);
                        uriList.add(new URI(new org.apache.commons.httpclient.URI(subtitleUrl, false, "UTF-8").toString()));
                    } catch (final URISyntaxException e) {
                        LogUtils.processException(logger, e);
                    }
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
        final List<URI> uriList = getURIList(action, UriListType.VIDEOS);
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
        final List<URI> uriList = getURIList(action, UriListType.VIDEOS);
        queueLinks(uriList);
    }

    private boolean isSubtitle() {
        return fileURL.matches("http://.*?dmcdn\\.net/static/.+?\\.srt/.+");
    }

    //reference : http://www.dailymotion.com/doc/api/obj-video.html#obj-video-cnx-subtitles
    private void queueSubtitles() throws Exception {
        final String videoId = getVideoIdFromURL();
        final String action = String.format("https://api.dailymotion.com/video/%s/subtitles", videoId);
        final List<URI> uriList = getURIList(action, UriListType.SUBTITLES);
        if (uriList.isEmpty()) {
            logger.info("No subtitles found");
        } else {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            logger.info(uriList.size() + " subtitles added");
        }
    }

    private void downloadSubtitle() throws Exception {
        //http://static2.dmcdn.net/static/video/339/362/35263933:subtitle_en.srt -> original subtitle url
        //http://static2.dmcdn.net/static/video/339/362/35263933:subtitle_en.srt/test subtitle in dailymotion?en -> title and language added at tail
        final String titleLanguage = URLDecoder.decode(fileURL.substring(fileURL.lastIndexOf("/") + 1), "UTF-8");
        final String title = titleLanguage.substring(0, titleLanguage.lastIndexOf(SUBTITLE_SEPARATOR));
        final String language = titleLanguage.substring(titleLanguage.lastIndexOf(SUBTITLE_SEPARATOR) + 1);
        httpFile.setFileName(title + "." + language + ".srt");
        fileURL = fileURL.substring(0, fileURL.lastIndexOf("/")); //remove "/"+title+language
        final HttpMethod method = getGetMethod(fileURL);
        setFileStreamContentTypes("text/plain");
        setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error downloading subtitle");
        }
    }

    private class DailyMotionVideo implements Comparable<DailyMotionVideo> {
        private final static int NEAREST_LOWER_PENALTY = 10;
        private final static int FLV_PENALTY = 1;
        private final VideoQuality videoQuality;
        private final String container;
        private final String url;
        private final int weight;

        private DailyMotionVideo(final VideoQuality videoQuality, final String container, final String url) {
            this.videoQuality = videoQuality;
            this.container = container;
            this.url = url;
            this.weight = calcWeight();
        }

        private int calcWeight() {
            final VideoQuality configQuality = config.getVideoQuality();
            final int deltaQ = videoQuality.getQuality() - configQuality.getQuality();
            int weight = (deltaQ < 0 ? Math.abs(deltaQ) + NEAREST_LOWER_PENALTY : deltaQ); //prefer nearest better if the same quality doesn't exist
            if (container.equalsIgnoreCase("FLV")) { //prefer MP4 on same quality
                weight += FLV_PENALTY;
            }
            return weight;
        }

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
