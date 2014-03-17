package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.rtmp.SwfVerificationHelper;
import cz.vity.freerapid.plugins.services.youtube.srt.Transcription2SrtUtil;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda, JPEXS, ntoskrnl
 * @since 0.82
 */
class YouTubeRunner extends AbstractRtmpRunner {
    private static final Logger logger = Logger.getLogger(YouTubeRunner.class.getName());

    private YouTubeSettingsConfig config;
    private int fmt = 0;
    private String fileExtension = ".flv";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setClientParameter(DownloadClientConsts.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; rv:12.0) Gecko/ 20120405 Firefox/14.0.1");
        addCookie(new Cookie(".youtube.com", "PREF", "hl=en", "/", 86400, false));
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        setClientParameter(DownloadClientConsts.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; rv:12.0) Gecko/ 20120405 Firefox/14.0.1");
        addCookie(new Cookie(".youtube.com", "PREF", "hl=en", "/", 86400, false));
        setConfig();

        if (checkSubtitles()) {
            return;
        }

        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            bypassAgeVerification(method);
            checkProblems();
            fileURL = method.getURI().toString();
            checkName();

            if (isUserPage()) {
                parseUserPage();
                return;
            }

            checkFmtParameter();
            checkName();

            final String fmtStreamMap = PlugUtils.getStringBetween(getContentAsString(), "\"url_encoded_fmt_stream_map\": \"", "\"");
            logger.info(fmtStreamMap);
            Matcher matcher = PlugUtils.matcher("([^,]*\\\\u0026itag=" + fmt + "[^,]*|[^,]*itag=" + fmt + "\\\\u0026[^,]*)", fmtStreamMap);
            if (!matcher.find()) {
                throw new PluginImplementationException("Cannot find specified video format (" + fmt + ")");
            }
            final String formatContent = matcher.group(1);
            if (formatContent.contains("rtmp")) {
                matcher = PlugUtils.matcher("conn=(.+?)(?:\\\\u0026.+)?$", formatContent);
                if (!matcher.find()) {
                    throw new PluginImplementationException("Cannot find stream address");
                }
                final String conn = URLDecoder.decode(matcher.group(1), "UTF-8");
                matcher = PlugUtils.matcher("stream=(.+?)(?:\\\\u0026.+)?$", formatContent);
                if (!matcher.find()) {
                    throw new PluginImplementationException("Cannot find stream params");
                }
                final String sparams = URLDecoder.decode(matcher.group(1), "UTF-8");
                final String swfUrl = PlugUtils.getStringBetween(getContentAsString(), "\"url\": \"", "\"").replace("\\/", "/");
                final RtmpSession rtmpSession = new RtmpSession(conn, sparams);
                rtmpSession.getConnectParams().put("swfUrl", swfUrl);
                rtmpSession.getConnectParams().put("pageUrl", fileURL);
                new SwfVerificationHelper(swfUrl).setSwfVerification(rtmpSession, client);
                if (!tryDownloadAndSaveFile(rtmpSession)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                matcher = PlugUtils.matcher("url=(.+?)(?:\\\\u0026.+)?$", formatContent);
                if (!matcher.find()) {
                    throw new PluginImplementationException("Cannot find stream URL");
                }
                String videoURL = matcher.group(1);
                if (!videoURL.contains("signature")) {
                    matcher = PlugUtils.matcher("sig=(.+?)(?:\\\\u0026.+)?$", formatContent);
                    if (matcher.find()) {
                        videoURL = videoURL + "&signature=" + matcher.group(1);
                    }
                }
                method = getGetMethod(URLDecoder.decode(URLDecoder.decode(videoURL, "UTF-8"), "UTF-8"));
                setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }


    private void checkProblems() throws Exception {
        if (getContentAsString().contains("video you have requested is not available") || getContentAsString().contains("video is no longer available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        /* Causes false positives
        final Matcher matcher = getMatcherAgainstContent("<div\\s+?class=\"yt-alert-content\">\\s*([^<>]+?)\\s*</div>");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(matcher.group(1));
        }
        */
    }

    private void checkName() throws ErrorDuringDownloadingException {
        try {
            PlugUtils.checkName(httpFile, getContentAsString(), "<meta name=\"title\" content=\"", "\"");
        } catch (final PluginImplementationException e) {
            PlugUtils.checkName(httpFile, getContentAsString(), "<title>", "- YouTube\n</title>");
        }
        String fileName = PlugUtils.unescapeHtml(PlugUtils.unescapeHtml(httpFile.getFileName()));
        if (!isUserPage()) {
            fileName += fileExtension;
        }
        httpFile.setFileName(fileName);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean isUserPage() {
        return fileURL.contains("/user/");
    }

    private void setConfig() throws Exception {
        YouTubeServiceImpl service = (YouTubeServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private void checkFmtParameter() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("fmt=(\\d+)", fileURL.toLowerCase(Locale.ENGLISH));

        if (matcher.find()) {
            final String fmtCode = matcher.group(1);

            if (fmtCode.length() <= 2) {
                fmt = Integer.parseInt(fmtCode);
                setFileExtension(fmt);
            }
        } else {
            processConfig();
        }
    }

    private void processConfig() throws ErrorDuringDownloadingException {
        String fmt_map = PlugUtils.getStringBetween(getContentAsString(), "\"fmt_list\": \"", "\"").replace("\\/", "/");
        //Example: 37/1920x1080/9/0/115,22/1280x720/9/0/115,35/854x480/9/0/115,34/640x360/9/0/115,5/320x240/7/0/0
        String[] formats = fmt_map.split(",");
        String[][] formatParts = new String[formats.length][];
        for (int f = 0; f < formats.length; f++) {
            //Example: 37/1920x1080/9/0/115
            formatParts[f] = formats[f].split("/");
        }
        int qualityWidth = config.getQualityWidth();
        int qualityIndex = -1;
        if (qualityWidth == YouTubeSettingsConfig.MAX_WIDTH) {
            logger.info("Selecting maximum quality");
            qualityIndex = 0;
        } else if (qualityWidth == YouTubeSettingsConfig.MIN_WIDTH) {
            logger.info("Selecting minimum quality");
            qualityIndex = formatParts.length - 1;
        } else {
            int nearestGreater = Integer.MAX_VALUE;
            int nearestGreaterIndex = -1;
            int nearestLower = Integer.MIN_VALUE;
            int nearestLowerIndex = -1;
            for (int f = 0; f < formatParts.length; f++) {
                String[] wh = formatParts[f][1].split("x");
                int h = Integer.parseInt(wh[1]);
                if (h == qualityWidth) {
                    qualityIndex = f;
                    break;
                } else {
                    if ((h > qualityWidth) && (nearestGreater > h)) {
                        nearestGreater = h;
                        nearestGreaterIndex = f;
                    }
                    if ((h < qualityWidth) && (nearestLower < h)) {
                        nearestLower = h;
                        nearestLowerIndex = f;
                    }
                }
            }
            if (qualityIndex == -1) {
                if (nearestLowerIndex != -1) {
                    qualityIndex = nearestLowerIndex;
                    logger.info("Selected quality not found, using nearest lower");
                } else {
                    qualityIndex = nearestGreaterIndex;
                    logger.info("Selected quality not found, using nearest better");
                }
            }
        }

        if (qualityIndex == -1) throw new PluginImplementationException("Cannot select quality");
        logger.info("Quality to download: fmt" + formatParts[qualityIndex][0] + " " + formatParts[qualityIndex][1]);
        fmt = Integer.parseInt(formatParts[qualityIndex][0]);
        setFileExtension(fmt);
    }

    private void setFileExtension(int fmtCode) {
        switch (fmtCode) {
            case 13:
            case 17:
                fileExtension = ".3gp";
                break;
            case 18:
            case 22:
            case 37:
            case 38:
                fileExtension = ".mp4";
                break;
            case 43:
                fileExtension = ".webm";
                break;
        }
    }

    private void parseUserPage() throws Exception {
        final String user = getUserFromUrl();
        final List<URI> uriList = new LinkedList<URI>();
        final int MAX_RESULTS = 50;
        for (int index = 1; ; index += MAX_RESULTS) {
            final HttpMethod method = getMethodBuilder()
                    .setReferer(null)
                    .setAction("http://gdata.youtube.com/feeds/api/users/" + user + "/uploads")
                    .setParameter("start-index", String.valueOf(index))
                    .setParameter("max-results", String.valueOf(MAX_RESULTS))
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final int previousSize = uriList.size();
            final Matcher matcher = getMatcherAgainstContent("<media:player url='(.+?)'/>");
            while (matcher.find()) {
                try {
                    final String link = PlugUtils.replaceEntities(matcher.group(1)).replace("&feature=youtube_gdata_player", "");
                    uriList.add(new URI(link));
                } catch (final URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
            if (uriList.size() - previousSize < MAX_RESULTS) {
                break;
            }
        }
        // YouTube returns the videos in descending date order, which is a bit illogical.
        // If the user wants them that way, don't reverse.
        if (!config.isReversePlaylistOrder()) {
            Collections.reverse(uriList);
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        logger.info(uriList.size() + " videos added");
        if (!uriList.isEmpty()) {
            httpFile.getProperties().put("removeCompleted", true);
        }
    }

    private String getUserFromUrl() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher(".+/([^\\?&#]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        return matcher.group(1);
    }

    private String getIdFromUrl() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("(?:[\\?&]v=|\\.be/)([^\\?&#]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        return matcher.group(1);
    }

    private boolean checkSubtitles() throws Exception {
        Matcher matcher = PlugUtils.matcher("#subtitles:(.*?):(.+)", fileURL);
        if (matcher.find()) {
            final String lang = matcher.group(1);
            if (!lang.isEmpty()) {
                fileExtension = "." + lang + ".srt";
            } else {
                fileExtension = ".srt";
            }
            runCheck();
            final String url = "http://www.youtube.com/api/timedtext?type=track" + matcher.group(2);
            final HttpMethod method = getGetMethod(url);
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            try {
                final byte[] converted = Transcription2SrtUtil.convert(getContentAsString()).getBytes("UTF-8");
                httpFile.setFileSize(converted.length);
                downloadTask.saveToFile(new ByteArrayInputStream(converted));
            } catch (final Exception e) {
                LogUtils.processException(logger, e);
                throw new PluginImplementationException("Error converting and saving subtitles", e);
            }
            return true;
        } else if (config.isDownloadSubtitles()) {
            final String id = getIdFromUrl();
            final HttpMethod method = getGetMethod("http://www.youtube.com/api/timedtext?type=list&v=" + id);
            if (makeRedirectedRequest(method)) {
                final List<URI> list = new LinkedList<URI>();
                matcher = getMatcherAgainstContent("<track id=\"\\d*\" name=\"(.*?)\" lang_code=\"(.*?)\"");
                while (matcher.find()) {
                    final String name = matcher.group(1);
                    final String lang = matcher.group(2);
                    final String url = fileURL + "#subtitles:" + lang + ":&v=" + id + "&name=" + name + "&lang=" + lang;
                    try {
                        list.add(new URI(url));
                    } catch (final URISyntaxException e) {
                        LogUtils.processException(logger, e);
                    }
                }
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
            }
        }
        return false;
    }

    private void bypassAgeVerification(HttpMethod method) throws Exception {
        if (method.getURI().toString().matches("https?://(www\\.)?youtube\\.com/verify_age.*")) {
            setClientParameter(DownloadClientConsts.USER_AGENT, "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)");
            method = getGetMethod(fileURL);
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            if (method.getURI().toString().matches("https?://(www\\.)?youtube\\.com/verify_controversy.*") || getContentAsString().contains("verify_controversy?action_confirm=1")) {
                method = getMethodBuilder()
                        .setBaseURL("http://www.youtube.com")
                        .setActionFromFormWhereActionContains("verify_controversy", true)
                        .toPostMethod();
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
            }
            setClientParameter(DownloadClientConsts.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; rv:12.0) Gecko/ 20120405 Firefox/14.0.1");
        } else if (getContentAsString().contains("I confirm that I am 18 years of age or older")) {
            if (!makeRedirectedRequest(getGetMethod(fileURL + "&has_verified=1"))) {
                throw new ServiceConnectionProblemException();
            }
        }
        if (getContentAsString().contains("Sign in to view this video")) {  //just in case they change age verification mechanism
            throw new PluginImplementationException("YouTube account is not supported : Sign in to view this video");
        }
    }

}