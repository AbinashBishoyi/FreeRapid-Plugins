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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Ramsestom, JPEXS, ntoskrnl, tong2shot
 */
class DailymotionRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DailymotionRunner.class.getName());
    private final static String SUBTITLE_SEPARATOR = "?";
    private final static String[] qualityUrlKeyMap = {"ldURL", "sdURL", "hqURL", "hd720URL"};
    private DailymotionSettingsConfig config;

    private static enum UriListType {VIDEOS, SUBTITLES}

    private void setConfig() throws Exception {
        DailymotionServiceImpl service = (DailymotionServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private String getQualityUrlKey(final int quality) {
        return qualityUrlKeyMap[quality];
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".dailymotion.com", "family_filter", "off", "/", 86400, false));
        addCookie(new Cookie(".dailymotion.com", "lang", "en_EN", "/", 86400, false));
        setFileStreamContentTypes(new String[0], new String[]{"application/json"});
        if (isPlaylist() || isGroup() || isSubtitle()) {
            //do nothing
        } else {
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
        httpFile.setFileName(httpFile.getFileName() + ".mp4");
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
        checkName();
        queueSubtitles();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            setConfig();
            final String sequence = urlDecode(PlugUtils.getStringBetween(getContentAsString(), "\"sequence\":\"", "\""));
            logger.info("Quality setting : " + config.getQualitySetting());
            final List<DailymotionVideo> dmvList = new LinkedList<DailymotionVideo>();
            for (int i = 0; i < qualityUrlKeyMap.length; i++) {
                final String urlKey = getQualityUrlKey(i);
                final String urlRegex = String.format("\"%s\":\"(.+?)\"", urlKey);
                final Matcher matcher = PlugUtils.matcher(urlRegex, sequence);
                if (matcher.find()) {
                    final String url = matcher.group(1);
                    DailymotionVideo dmv = new DailymotionVideo(i, urlKey, url);
                    logger.info(dmv.toString());
                    dmvList.add(dmv);
                }
            }
            if (dmvList.isEmpty()) throw new PluginImplementationException("Unable to find video URL");
            final String url = Collections.min(dmvList).url;
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

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("We can't find the page you're looking for")
                || contentAsString.contains("video has been removed")
                || contentAsString.contains("Page Gone")
                || contentAsString.contains("\"message\":\"Can not find the object")
                || contentAsString.contains("\"message\":\"This video has been censored.\"")) {
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
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            if (uriListType == UriListType.VIDEOS) {
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
                        final String title = httpFile.getFileName().replace(".mp4", "");
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
        final Matcher matcher = PlugUtils.matcher("/video/([^_#/]+)", fileURL);
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
        return fileURL.contains("dmcdn.net/static/");
    }

    //reference : http://www.dailymotion.com/doc/api/obj-video.html#obj-video-cnx-subtitles
    private void queueSubtitles() throws Exception {
        final String videoId = getVideoIdFromURL();
        final String action = String.format("https://api.dailymotion.com/video/%s/subtitles", videoId);
        final List<URI> uriList = getURIList(action, UriListType.SUBTITLES);
        if (uriList.isEmpty()) {
            logger.warning("No subtitles found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        logger.info(uriList.size() + " subtitles added");

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

    private class DailymotionVideo implements Comparable<DailymotionVideo> {
        private final int quality;
        private final String qualityUrlKey;
        private final String url;
        private int weight;

        private DailymotionVideo(int quality, String qualityUrlKey, String url) {
            this.quality = quality;
            this.qualityUrlKey = qualityUrlKey;
            this.url = url;
            calcWeight();
        }

        private void calcWeight() {
            final int configQuality = config.getQualitySetting();
            weight = ((quality - configQuality) < 0) ? (Math.abs(quality - configQuality) + 10) : (quality - configQuality); //+10 = nearest lower penalty, prefer nearest better if the same quality doesn't exist
        }

        @Override
        public int compareTo(DailymotionVideo that) {
            return Integer.valueOf(this.weight).compareTo(that.weight);
        }

        @Override
        public String toString() {
            return "weight         = " + weight + "\n" +
                    "quality        = " + quality + "\n" +
                    "qualityUrlKey  = " + qualityUrlKey + "\n" +
                    "url            = " + url;
        }
    }

}
