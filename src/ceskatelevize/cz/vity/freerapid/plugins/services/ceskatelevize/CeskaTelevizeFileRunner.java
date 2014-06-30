package cz.vity.freerapid.plugins.services.ceskatelevize;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import jlibs.core.net.URLUtil;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author JPEXS
 * @author tong2shot
 */
class CeskaTelevizeFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(CeskaTelevizeFileRunner.class.getName());
    private final static String FNAME = "fname";
    private final static String SWITCH_ITEM_ID = "switchitemid";
    private final static String DEFAULT_EXT = ".flv";
    private String switchItemIdFromUrl = null;
    private CeskaTelevizeSettingsConfig config;

    private void setConfig() throws Exception {
        CeskaTelevizeServiceImpl service = (CeskaTelevizeServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private boolean isBonus(String fileUrl) {
        return fileUrl.contains("/bonus/");
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (isArchiveEpisode(fileURL) || isArchiveProgramme(fileURL) || isMultiparts(fileURL)) {
            return;
        }
        final GetMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        checkName();
    }

    private void checkName() throws Exception {
        String filename;
        Matcher matcher;
        String content;

        matcher = getMatcherAgainstContent("(?i)charset\\s*=\\s*windows-1250");
        if (matcher.find()) {
            setPageEncoding("Windows-1250"); //usually they use "UTF-8" charset, but sometimes they use "windows-1250" charset
            GetMethod method = getGetMethod(fileURL);
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            setPageEncoding("UTF-8");
        }
        content = getContentAsString();

        if (content.contains("<h1 id=\"nazev\">")) {
            matcher = getMatcherAgainstContent("<h1 id=\"nazev\">(?:<a[^<>]+>)?(.+?)(?:</a>)?</h1>");
            if (!matcher.find()) {
                throw new PluginImplementationException("Error getting programme title (1)");
            }
            filename = matcher.group(1).replace("<span id=\"dil\">", "").replace("</span>", "").trim();
            if (content.contains("<h2 id=\"nazevcasti\">")) {
                matcher = getMatcherAgainstContent("<h2 id=\"nazevcasti\">(?:<a[^<>]+>)?(.+?)(?:</a>)?</h2>");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Error getting episode name (1)");
                }
                filename += " - " + matcher.group(1).trim();
            }
        } else if (content.contains("id=\"programmeInfoView\"")) {
            matcher = getMatcherAgainstContent("(?s)\"programmeInfoView\".+?<h2>(?:<a[^<>]+>)?(.+?)(?:</a>)?</h2>");
            if (!matcher.find()) {
                throw new PluginImplementationException("Error getting programme title (2)");
            }
            filename = matcher.group(1).trim();
            if (content.contains("\"episode-title\"")) {
                matcher = getMatcherAgainstContent("\"episode-title\">(.+?)</");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Error getting episode name (2)");
                }
                filename += " - " + matcher.group(1).trim();
            }
        } else if (content.contains("id=\"global\"")) {
            matcher = getMatcherAgainstContent("(?s)id=\"global\".+?<h1>(?:<a[^<>]+>)?(.+?)(?:</a>)?</h1>");
            if (!matcher.find()) {
                throw new PluginImplementationException("Error getting programme title (3)");
            }
            filename = matcher.group(1).trim();
            if (content.contains("id=\"titleBox\"")) {
                matcher = getMatcherAgainstContent("<h2>(?:<a[^<>]+>)?(.+?)(?:</a>)?</h2>");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Error getting episode name (3)");
                }
                filename += " - " + matcher.group(1).trim();
            }
        } else if (content.contains("<title>")) {
            matcher = getMatcherAgainstContent("<title>(.+?)</title>");
            if (!matcher.find()) {
                throw new PluginImplementationException("Error getting programme title (4)");
            }
            filename = PlugUtils.unescapeHtml(matcher.group(1).trim())
                    .replace("Video —", "")
                    .replace("— Česká televize", "")
                    .replace("— iVysílání", "");
        } else {
            throw new PluginImplementationException("Error getting programme title (5)");
        }

        filename = filename.replaceAll("[\\t\\n]", "");
        filename += DEFAULT_EXT;
        httpFile.setFileName(filename);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        if (isArchiveEpisode(fileURL)) {
            processArchiveEpisode();
        } else if (isMultiparts(fileURL)) {
            processMultiparts();
        } else {
            final GetMethod method = getGetMethod(fileURL);
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("");
            }
            checkProblems();
            if (isArchiveProgramme(fileURL)) {
                parseArchiveProgramme(getContentAsString());
                return;
            }
            checkName();
        }

        HttpMethod httpMethod;
        String referer = fileURL;
        if (!getContentAsString().contains("getPlaylistUrl(")) {
            Matcher matcher = Pattern.compile("(<i?frame(?:.*?)src\\s?=\\s?(?:\"|')(.+?)(?:\"|')(?:.*?)>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(getContentAsString());
            String action = null;
            int start = 0;
            final String lower = "iFramePlayer".toLowerCase();
            while (matcher.find(start)) {
                final String content = matcher.group(1);
                if (content.toLowerCase().contains(lower)) {
                    final String iFrameUrl = PlugUtils.replaceEntities(matcher.group(2));
                    if ((!isBonus(fileURL) && !iFrameUrl.contains("bonus=")) || (isBonus(fileURL) && iFrameUrl.contains("bonus="))) {
                        action = iFrameUrl;
                        break;
                    }
                }
                start = matcher.end();
            }
            if (action == null) {
                throw new PluginImplementationException("Error getting playlist URL");
            }

            httpMethod = getMethodBuilder().setReferer(referer).setAction(action).toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            referer = httpMethod.getURI().toString();

            if (!getContentAsString().contains("getPlaylistUrl(")) {
                httpMethod = getMethodBuilder().setReferer(referer).setActionFromAHrefWhereATagContains("Přehrát video").toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                referer = httpMethod.getURI().toString();
            }
        }

        URL requestUrl = new URL(referer);
        String videoId;
        String type;
        try {
            videoId = PlugUtils.getStringBetween(getContentAsString(), "\"id\":\"", "\"");
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("Video ID not found");
        }
        try {
            type = PlugUtils.getStringBetween(getContentAsString(), "\"type\":\"", "\"");
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("Request type not found");
        }
        httpMethod = getMethodBuilder()
                .setReferer(referer)
                .setAjax()
                .setAction("http://www.ceskatelevize.cz/ivysilani/ajax/get-playlist-url")
                .setParameter("playlist[0][id]", videoId)
                .setParameter("playlist[0][startTime]", "")
                .setParameter("playlist[0][stopTime]", "")
                .setParameter("playlist[0][type]", type)
                .setParameter("requestSource", "iVysilani")
                .setParameter("requestUrl", requestUrl.getAuthority())
                .setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .setHeader("x-addr", "127.0.0.1")
                .toPostMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Cannot load playlist URL");
        }
        checkProblems();

        Matcher matcher = getMatcherAgainstContent("\"url\":\"(.+?)\"");
        if (!matcher.find()) {
            throw new PluginImplementationException("Playlist URL not found");
        }
        String playlistUrl = URLDecoder.decode(matcher.group(1).replace("\\/", "/"), "UTF-8").replace("hashedId", "id");
        httpMethod = new GetMethod(playlistUrl);
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Cannot connect to playlist");
        }
        checkProblems();
        setConfig();
        List<SwitchItem> switchItems = getSwitchItems(getContentAsString());
        if (switchItems.size() == 1) {
            SwitchItem selectedSwitchItem = switchItems.get(0);
            logger.info("Selected switch item : " + selectedSwitchItem);
            Video selectedVideo = getSelectedVideo(selectedSwitchItem);
            RtmpSession rtmpSession = new RtmpSession(selectedSwitchItem.getBase(), selectedVideo.getSrc());
            rtmpSession.disablePauseWorkaround();
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            queueSwitchItems(switchItems);
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Neexistuj") || contentAsString.contains("Stránka nenalezena")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("content is not available at")
                || contentAsString.contains("\"url\":\"error_region\"")) {
            throw new PluginImplementationException("This content is not available at your territory due to limited copyright");
        }
        if (contentAsString.contains("došlo k chybě při práci s databází")) {
            throw new ServiceConnectionProblemException("An error occurred while working with databases");
        }
    }

    private List<SwitchItem> getSwitchItems(String playlistContent) throws PluginImplementationException {
        final Matcher switchMatcher = Pattern.compile("<switchItem id=\"([^\"]+)\" base=\"([^\"]+)\" begin=\"([^\"]+)\" duration=\"([^\"]+)\" clipBegin=\"([^\"]+)\".*?>\\s*(<video[^>]*>\\s*)*</switchItem>", Pattern.MULTILINE + Pattern.DOTALL).matcher(playlistContent);
        logger.info("Available switch items : ");
        List<SwitchItem> switchItems = new ArrayList<SwitchItem>();
        while (switchMatcher.find()) {
            String swItemId = switchMatcher.group(1);
            if ((switchItemIdFromUrl != null) && (!swItemId.equals(switchItemIdFromUrl))) {
                continue;
            }
            String swItemText = switchMatcher.group(0);
            String base = PlugUtils.replaceEntities(switchMatcher.group(2));
            double duration = Double.parseDouble(switchMatcher.group(4));
            SwitchItem newItem = new SwitchItem(swItemId, base, duration);
            Matcher videoMatcher = Pattern.compile("<video src=\"([^\"]+)\" system-bitrate=\"([0-9]+)\" label=\"([0-9]+)p\" enabled=\"true\" */>").matcher(swItemText);
            while (videoMatcher.find()) {
                newItem.addVideo(new Video(videoMatcher.group(1), Integer.parseInt(videoMatcher.group(3))));
            }
            switchItems.add(newItem);
            logger.info(newItem.toString());
        }
        if (switchItems.size() > 1) {
            for (int i = 0; i < switchItems.size(); ) {
                SwitchItem switchItem = switchItems.get(i);
                if (switchItem.getDuration() <= 60) {  //assumption: advertisement<=60s, content>60s
                    switchItems.remove(i); //remove advertisement
                } else {
                    i++;
                }
            }
        }
        if (switchItems.isEmpty()) {
            throw new PluginImplementationException("No stream found");
        }
        return switchItems;
    }

    private Video getSelectedVideo(SwitchItem switchItem) throws PluginImplementationException {
        Video selectedVideo = null;
        logger.info("Config settings : " + config);
        if (config.getVideoQuality() == VideoQuality.Highest) {
            selectedVideo = Collections.max(switchItem.getVideos());
        } else if (config.getVideoQuality() == VideoQuality.Lowest) {
            selectedVideo = Collections.min(switchItem.getVideos());
        } else {
            final int LOWER_QUALITY_PENALTY = 10;
            int weight = Integer.MAX_VALUE;
            for (Video video : switchItem.getVideos()) {
                int deltaQ = video.getVideoQuality() - config.getVideoQuality().getQuality();
                int tempWeight = (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ);
                if (tempWeight < weight) {
                    weight = tempWeight;
                    selectedVideo = video;
                }
            }
        }
        if (selectedVideo == null) {
            throw new PluginImplementationException("Cannot select video");
        }
        logger.info("Video to be downloaded : " + selectedVideo);
        return selectedVideo;
    }

    private void queueSwitchItems(List<SwitchItem> switchItems) throws Exception {
        List<URI> list = new LinkedList<URI>();
        for (int i = 0; i < switchItems.size(); i++) {
            SwitchItem switchItem = switchItems.get(i);
            String switchIdParam = SWITCH_ITEM_ID + "=" + URLEncoder.encode(switchItem.getId(), "UTF-8");
            String fnameParam = "&" + FNAME + "=" + URLEncoder.encode(httpFile.getFileName().replaceFirst(Pattern.quote(DEFAULT_EXT) + "$", "-" + (i + 1)), "UTF-8");
            try {
                list.add(new URI(fileURL + (fileURL.endsWith("/") ? "?" : "/?") + switchIdParam + fnameParam));
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No switch items available");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
        logger.info(list.size() + " switch items added");
    }

    private boolean isArchiveEpisode(String fileUrl) {
        return fileUrl.matches("http://decko\\.ceskatelevize\\.cz/player\\?.+");
    }

    private void processArchiveEpisode() throws Exception {
        URL url = new URL(fileURL);
        String filename = null;
        try {
            filename = URLUtil.getQueryParams(url.toString(), "UTF-8").get(FNAME);
        } catch (Exception e) {
            //
        }
        if (filename == null) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(URLDecoder.decode(filename, "UTF-8") + DEFAULT_EXT, "_"));
        fileURL = fileURL.replaceFirst("&" + FNAME + "=.+", "");

        HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
    }

    private boolean isArchiveProgramme(String fileUrl) {
        return fileUrl.matches("http://decko\\.ceskatelevize\\.cz/[^_#/?]+/?");
    }

    private void parseArchiveProgramme(String content) throws Exception {
        Matcher matcher = PlugUtils.matcher("var IDEC\\s*?=\\s*?'([^']+?)'", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("Episode ID not found");
        }
        String idec = matcher.group(1).trim();
        HttpMethod method = getMethodBuilder()
                .setReferer(fileURL)
                .setAjax()
                .setAction("http://decko.ceskatelevize.cz/rest/Programme/relatedVideosForEpisode")
                .setAndEncodeParameter("IDEC", idec)
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();

        List<URI> list = new LinkedList<URI>();
        matcher = getMatcherAgainstContent("\"title\":\"([^\"]+?)\".+?\"programmeTitle\":\"([^\"]+?)\".+?\"IDEC\":\"([^\"]+?)\"");
        while (matcher.find()) {
            String episodeTitle = matcher.group(1).trim();
            String programmeTitle = matcher.group(2).trim();
            idec = matcher.group(3).trim();
            String fname = programmeTitle + " - " + episodeTitle;
            String episodeUrl = String.format("http://decko.ceskatelevize.cz/player?width=560&IDEC=%s&%s=%s", URLEncoder.encode(idec, "UTF-8"), FNAME, URLEncoder.encode(fname, "UTF-8"));
            try {
                list.add(new URI(episodeUrl));
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No episodes available");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
        logger.info(list.size() + " episodes added");
    }

    private boolean isMultiparts(String fileUrl) {
        return fileUrl.contains("?" + SWITCH_ITEM_ID + "=");
    }

    private void processMultiparts() throws Exception {
        URL url = new URL(fileURL);
        String filename = null;
        try {
            filename = URLUtil.getQueryParams(url.toString(), "UTF-8").get(FNAME);
        } catch (Exception e) {
            //
        }
        if (filename == null) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(URLDecoder.decode(filename, "UTF-8") + DEFAULT_EXT, "_"));

        switchItemIdFromUrl = null;
        try {
            switchItemIdFromUrl = URLUtil.getQueryParams(fileURL, "UTF-8").get(SWITCH_ITEM_ID);
        } catch (Exception e) {
            //
        }
        if (switchItemIdFromUrl == null) {
            throw new PluginImplementationException("Switch item ID param not found");
        }

        fileURL = fileURL.replaceFirst("\\?" + SWITCH_ITEM_ID + "=.+", "");
        HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
    }
}
