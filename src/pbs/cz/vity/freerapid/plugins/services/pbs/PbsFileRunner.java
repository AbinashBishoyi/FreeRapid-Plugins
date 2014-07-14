package cz.vity.freerapid.plugins.services.pbs;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 * @author tong2shot (subtitle)
 */
class PbsFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(PbsFileRunner.class.getName());
    private final static byte[] DECRYPT_KEY = "RPz~i4p*FQmx>t76".getBytes(Charset.forName("UTF-8"));
    private final static String DEFAULT_EXT = ".flv";
    private SettingsConfig config;

    private void setConfig() throws Exception {
        final PbsServiceImpl service = (PbsServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final String url = "http://video.pbs.org/videoPlayerInfo/" + getId() + "/";
        final HttpMethod method = getGetMethod(url);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getId() throws Exception {
        final Matcher matcher = PlugUtils.matcher("http://video\\.pbs\\.org/video/(\\d+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        return matcher.group(1);
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String name = PlugUtils.getStringBetween(getContentAsString(), "<title>", "</title>");
        httpFile.setFileName(name.replace(": ", " - ") + DEFAULT_EXT);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();

        String content = getContentAsString();
        setConfig();
        if (config.isDownloadSubtitles()) {
            downloadSubtitle(content);
        }

        final String url = getUrl(content);
        final HttpMethod method = getGetMethod(url);
        makeRequest(method);
        checkProblems();

        final Header location = method.getResponseHeader("Location");
        if (location == null) {
            throw new PluginImplementationException("No redirect location");
        }
        final String[] rtmpData = location.getValue().split("mp4:");
        if (rtmpData.length != 2) {
            throw new PluginImplementationException("Error parsing RTMP URL");
        }
        final RtmpSession rtmpSession = new RtmpSession(rtmpData[0], "mp4:" + rtmpData[1]);
        rtmpSession.getConnectParams().put("pageUrl", fileURL);
        tryDownloadAndSaveFile(rtmpSession);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("We were unable to find the page that was requested")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("unavailable in your region")) {
            throw new NotRecoverableDownloadException("This video is not available in your region");
        }
        if (getContentAsString().contains("Media is not available")) {
            throw new PluginImplementationException("Media is not available");
        }
    }

    private String getUrl(String content) throws Exception {
        final String releaseUrl = PlugUtils.getStringBetween(content, "<releaseURL>", "</releaseURL>");
        final String[] data = releaseUrl.split("\\$");
        if (data.length != 3) {
            throw new PluginImplementationException("Error parsing 'releaseURL'");
        }
        final byte[] iv = Hex.decodeHex(data[1].toCharArray());
        final byte[] cipherText = Base64.decodeBase64(data[2]);
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(DECRYPT_KEY, "AES"), new IvParameterSpec(iv));
        return new String(cipher.doFinal(cipherText), "UTF-8");
    }

    private void downloadSubtitle(String content) {
        Matcher captionMatcher = PlugUtils.matcher("<caption>.*?<url>(.+?)</url>.*?<language>(.+?)</language>.*?<format>(.+?)</format>.*?</caption>", content);
        List<Caption> captions = new LinkedList<Caption>();
        while (captionMatcher.find()) {
            String captionUrl = captionMatcher.group(1).trim();
            String captionLang = captionMatcher.group(2).trim();
            String captionFormat = captionMatcher.group(3).trim();
            if (!captionLang.equalsIgnoreCase("en")) { //skip non EN subs
                continue;
            }
            captions.add(new Caption(captionUrl, captionFormat));
        }

        if (captions.isEmpty()) {
            logger.warning("No subtitles found");
        } else {
            int weight = Integer.MIN_VALUE;
            String subtitleUrl = null;
            for (Caption caption : captions) {
                String captionFormat = caption.format.toLowerCase(Locale.ENGLISH);
                int tempWeight = Integer.MIN_VALUE;
                if (captionFormat.contains("srt")) { //srt > sami > dfxp
                    tempWeight = 50;
                } else if (captionFormat.contains("sami")) {
                    tempWeight = 49;
                } else if (captionFormat.contains("dfxp")) {
                    tempWeight = 48;
                }
                if (tempWeight > weight) {
                    weight = tempWeight;
                    subtitleUrl = caption.url;
                }
            }

            if (subtitleUrl == null) {
                logger.warning("Cannot select subtitle");
            } else {
                SubtitleDownloader sbDownloader = new SubtitleDownloader();
                try {
                    sbDownloader.downloadSubtitle(client, httpFile, subtitleUrl);
                } catch (Exception e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
    }

    private class Caption {
        private final String url;
        private final String format;

        private Caption(String url, String format) {
            this.url = url;
            this.format = format;
        }
    }

}