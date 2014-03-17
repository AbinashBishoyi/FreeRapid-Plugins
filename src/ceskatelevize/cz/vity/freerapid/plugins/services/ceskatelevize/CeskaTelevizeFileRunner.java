package cz.vity.freerapid.plugins.services.ceskatelevize;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
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
    private CeskaTelevizeSettingsConfig config;

    private void setConfig() throws Exception {
        CeskaTelevizeServiceImpl service = (CeskaTelevizeServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkName() throws Exception {
        String filename;
        Matcher matcher;
        String content;

        matcher = getMatcherAgainstContent("(?i)charset\\s*=\\s*windows-1250");
        if (matcher.find()) {
            setPageEncoding("Windows-1250"); //sometimes they use "windows-1250" charset
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
            filename = matcher.group(1).trim();
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
            filename = matcher.group(1).trim()
                    .replace("Video &mdash;", "")
                    .replace("Video —", "")
                    .replace("&mdash; &#268;esk&aacute; televize", "")
                    .replace("— Česká televize", "")
                    .replace("— iVysílání", "");
        } else {
            throw new PluginImplementationException("Error getting programme title (5)");
        }

        filename = filename.replaceAll("[\\t\\n]", "");
        filename += ".flv";
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
            checkName();

            HttpMethod httpMethod;
            String referer = fileURL;
            if (!getContentAsString().contains("getPlaylistUrl(")) {
                httpMethod = getMethodBuilder().setReferer(referer).setActionFromIFrameSrcWhereTagContains("iFramePlayer").toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                referer = httpMethod.getURI().toString();
            }

            if (!getContentAsString().contains("getPlaylistUrl(")) {
                httpMethod = getMethodBuilder().setReferer(referer).setActionFromAHrefWhereATagContains("Přehrát video").toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                referer = httpMethod.getURI().toString();
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
            SwitchItem selectedSwitchItem = getSelectedSwitchItem(getContentAsString());
            Video selectedVideo = getSelectedVideo(selectedSwitchItem);
            RtmpSession rtmpSession = new RtmpSession(selectedSwitchItem.getBase(), selectedVideo.getSrc());
            rtmpSession.disablePauseWorkaround();
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
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
    }

    private SwitchItem getSelectedSwitchItem(String playlistContent) throws PluginImplementationException {
        final Matcher switchMatcher = Pattern.compile("<switchItem id=\"([^\"]+)\" base=\"([^\"]+)\" begin=\"([^\"]+)\" duration=\"([^\"]+)\" clipBegin=\"([^\"]+)\".*?>\\s*(<video[^>]*>\\s*)*</switchItem>", Pattern.MULTILINE + Pattern.DOTALL).matcher(playlistContent);
        logger.info("Available switch items : ");
        List<SwitchItem> switchItems = new ArrayList<SwitchItem>();
        while (switchMatcher.find()) {
            String swItemText = switchMatcher.group(0);
            String base = PlugUtils.replaceEntities(switchMatcher.group(2));
            double duration = Double.parseDouble(switchMatcher.group(4));
            SwitchItem newItem = new SwitchItem(base, duration);
            Matcher videoMatcher = Pattern.compile("<video src=\"([^\"]+)\" system-bitrate=\"([0-9]+)\" label=\"([0-9]+)p\" enabled=\"true\" */>").matcher(swItemText);
            while (videoMatcher.find()) {
                newItem.addVideo(new Video(videoMatcher.group(1), Integer.parseInt(videoMatcher.group(3))));
            }
            switchItems.add(newItem);
            logger.info(newItem.toString());
        }
        if (switchItems.isEmpty()) {
            throw new PluginImplementationException("No stream found.");
        }
        SwitchItem selectedSwitchItem = Collections.max(switchItems); //switch item with the longest duration
        logger.info("Selected switch item : " + selectedSwitchItem);
        return selectedSwitchItem;
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
}