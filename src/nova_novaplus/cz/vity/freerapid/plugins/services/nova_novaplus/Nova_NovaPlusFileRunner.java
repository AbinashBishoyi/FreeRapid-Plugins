package cz.vity.freerapid.plugins.services.nova_novaplus;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u4
 */
class Nova_NovaPlusFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(Nova_NovaPlusFileRunner.class.getName());
    private final static String TIME_SERVICE_URL = "http://tn.nova.cz/lbin/time.php";
    private SettingsConfig config;

    private void setConfig() throws Exception {
        Nova_NovaPlusServiceImpl service = (Nova_NovaPlusServiceImpl) getPluginService();
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
        PlugUtils.checkName(httpFile, content, "<h1>", "</h1>");
        httpFile.setFileName(httpFile.getFileName() + ".flv");
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
            Matcher matcher = PlugUtils.matcher("src=\"(http://[^\"]+?config\\.php[^\"]+?)\"", getContentAsString());
            if (!matcher.find()) {
                throw new PluginImplementationException("Config URL not found");
            }
            String configUrl = matcher.group(1);
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(configUrl).toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            String base64Config;
            try {
                base64Config = PlugUtils.getStringBetween(getContentAsString(), "'", "';");
            } catch (PluginImplementationException e) {
                throw new PluginImplementationException("Base64 config not found");
            }
            String configDecrypted = new Crypto().decrypt(base64Config);
            String mediaListContent = getMediaListContent(configDecrypted);
            setConfig();
            Nova_NovaPlusVideo selectedVideo = getSelectedVideo(mediaListContent);
            RtmpSession rtmpSession = new RtmpSession(selectedVideo.baseUrl, selectedVideo.url);
            if (!tryDownloadAndSaveFile(rtmpSession)) {
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
        if (contentAsString.contains("ale hledáte stránku, která neexistuje")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String getTimeString() throws Exception {
        if (!makeRequest(getGetMethod(TIME_SERVICE_URL))) {
            throw new PluginImplementationException("Time service not available");
        }
        return getContentAsString().substring(0, 14);
    }

    private String getMediaListContent(String configDecrypted) throws Exception {
        String timeStr = getTimeString();
        String mediaId;
        String resolverContent;
        String serviceUrl;
        String appId;
        String secret;
        try {
            mediaId = PlugUtils.getStringBetween(configDecrypted, "\"mediaId\":", ",");
            resolverContent = PlugUtils.getStringBetween(configDecrypted, "\"nacevi-resolver\":{", "},");
            serviceUrl = PlugUtils.getStringBetween(resolverContent, "\"serviceUrl\":\"", "\"").replace("\\/", "/");
            appId = PlugUtils.getStringBetween(resolverContent, "\"appId\":\"", "\"");
            secret = PlugUtils.getStringBetween(resolverContent, "\"secret\":\"", "\"");
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("Error parsing media config content");
        }
        String hashString = appId + "|" + mediaId + "|" + timeStr + "|" + secret;
        String base64FromBA = Base64.encodeBase64String(DigestUtils.md5(hashString));
        HttpMethod method = getMethodBuilder()
                .setAction(serviceUrl)
                .setParameter("c", appId + "|" + mediaId)
                .setParameter("h", "0")
                .setParameter("t", timeStr)
                .setParameter("s", base64FromBA)
                .setParameter("tm", "nova")
                .setParameter("d", "1")
                .setEncodeParameters(true)
                .toGetMethod();
        if (!makeRedirectedRequest(method) || !getContentAsString().contains("<status>Ok</status>")) {
            throw new PluginImplementationException("Error getting media list content");
        }
        return getContentAsString();
    }

    private Nova_NovaPlusVideo getSelectedVideo(String content) throws PluginImplementationException {
        List<Nova_NovaPlusVideo> videoList = new ArrayList<Nova_NovaPlusVideo>();
        String baseUrl = PlugUtils.getStringBetween(content, "<baseUrl>", "</baseUrl>").replace("<![CDATA[", "").replace("]]>", "");
        Matcher mediaMatcher = PlugUtils.matcher("(?s)<media>(.+?)</media>", content);
        logger.info("Available videos :");
        while (mediaMatcher.find()) {
            String mediaContent = mediaMatcher.group(1);
            Matcher qualityMatcher = PlugUtils.matcher("(?s)<quality>(.+?)</quality>", mediaContent);
            Matcher urlMatcher = PlugUtils.matcher("(?s)<url>(.+?)</url>", mediaContent);
            if (!qualityMatcher.find() || !urlMatcher.find()) {
                throw new PluginImplementationException("Error parsing media");
            }
            String quality = qualityMatcher.group(1).replace("<![CDATA[", "").replace("]]>", "");
            String url = urlMatcher.group(1).replace("<![CDATA[", "").replace("]]>", "");
            VideoQuality videoQuality;
            if (quality.contains("hd")) {
                videoQuality = VideoQuality.HD;
            } else if (quality.contains("hq")) {
                videoQuality = VideoQuality.HQ;
            } else {
                videoQuality = VideoQuality.LQ;
            }
            Nova_NovaPlusVideo novaPlusVideo = new Nova_NovaPlusVideo(videoQuality, baseUrl, url);
            logger.info(novaPlusVideo.toString());
            videoList.add(novaPlusVideo);
        }
        if (videoList.isEmpty()) {
            throw new PluginImplementationException("No available videos");
        }
        Nova_NovaPlusVideo selectedVideo = Collections.min(videoList);
        logger.info("Config settings : " + config);
        logger.info("Selected video  : " + selectedVideo);
        return selectedVideo;
    }

    private class Nova_NovaPlusVideo implements Comparable<Nova_NovaPlusVideo> {
        private final static int LOWER_QUALITY_PENALTY = 10;
        private final VideoQuality videoQuality;
        private final String baseUrl;
        private final String url;
        private int weight;

        public Nova_NovaPlusVideo(final VideoQuality videoQuality, String baseUrl, final String url) {
            this.videoQuality = videoQuality;
            this.baseUrl = baseUrl;
            this.url = url;
            calcWeight();
        }

        private void calcWeight() {
            VideoQuality configQuality = config.getVideoQuality();
            int deltaQ = videoQuality.getQuality() - configQuality.getQuality();
            weight = (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ); //prefer nearest better if the same quality doesn't exist
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public int compareTo(final Nova_NovaPlusVideo that) {
            return Integer.valueOf(this.weight).compareTo(that.weight);
        }

        @Override
        public String toString() {
            return "Nova_NovaPlusVideo{" +
                    "videoQuality=" + videoQuality +
                    ", baseUrl='" + baseUrl + '\'' +
                    ", url='" + url + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }

}
