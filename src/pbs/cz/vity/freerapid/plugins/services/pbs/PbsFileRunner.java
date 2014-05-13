package cz.vity.freerapid.plugins.services.pbs;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.tunlr.Tunlr;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import jlibs.core.net.URLUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final static String SUBTITLE_FNAME = "fname";
    private SettingsConfig config;

    private void setConfig() throws Exception {
        final PbsServiceImpl service = (PbsServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (isSubtitle(fileURL)) {
            return;
        }
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
        if (isSubtitle(fileURL)) {
            downloadSubtitle();
            return;
        }
        runCheck();

        setConfig();
        if (config.isDownloadSubtitles()) {
            queueSubtitle(getContentAsString());
        }

        final String url = getUrl();
        final HttpMethod method = getGetMethod(url);
        if (!client.getSettings().isProxySet()) {
            Tunlr.setupMethod(method);
        }
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

    private String getUrl() throws Exception {
        final String releaseUrl = PlugUtils.getStringBetween(getContentAsString(), "<releaseURL>", "</releaseURL>");
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

    private boolean isSubtitle(String fileURL) {
        return fileURL.contains("cdn.pbs.org/captions/");
    }

    private void queueSubtitle(String content) throws Exception {
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
            logger.warning("No subtitles found (1)");
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
                String subtitleFnameUrl = subtitleUrl + "?" + SUBTITLE_FNAME + "="
                        + URLEncoder.encode(httpFile.getFileName().replaceFirst(Pattern.quote(DEFAULT_EXT) + "$", ""), "UTF-8"); //add fname param
                List<URI> uriList = new LinkedList<URI>();
                uriList.add(new URI(new org.apache.commons.httpclient.URI(subtitleFnameUrl, false, "UTF-8").toString()));
                if (uriList.isEmpty()) {
                    logger.warning("No subtitles found (2)");
                } else {
                    getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
                }
            }
        }
    }

    private void downloadSubtitle() throws Exception {
        URL url = new URL(fileURL);
        String filename = null;
        try {
            filename = URLUtil.getQueryParams(url.toString(), "UTF-8").get(SUBTITLE_FNAME);
        } catch (Exception e) {
            //
        }
        if (filename == null) {
            throw new PluginImplementationException("File name not found");
        }
        String path = url.getPath();
        fileURL = url.getProtocol() + "://" + url.getAuthority() + path;
        String fileExt = path.substring(path.lastIndexOf("."));

        if (fileExt.equals(".srt") || fileExt.equals(".sami") || fileExt.equals(".smi")) {
            httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(URLDecoder.decode(filename, "UTF-8") + fileExt, "_"));
            final HttpMethod method = getGetMethod(fileURL);
            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
            setFileStreamContentTypes("application/smil+xml", "text/plain");
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error downloading subtitle");
            }
        } else if (fileExt.equals(".dfxp")) {
            httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(URLDecoder.decode(filename, "UTF-8") + ".srt", "_"));
            HttpMethod method = getGetMethod(fileURL);
            setTextContentTypes("application/ttaf+xml");
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            final byte[] subtitle = TimedText2Srt.convert(getContentAsString()).getBytes("UTF-8");
            httpFile.setFileSize(subtitle.length);
            try {
                downloadTask.saveToFile(new ByteArrayInputStream(subtitle));
            } catch (Exception e) {
                LogUtils.processException(logger, e);
                throw new PluginImplementationException("Error saving subtitle", e);
            }
        } else {
            throw new PluginImplementationException("Unknown subtitle type: " + fileExt);
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