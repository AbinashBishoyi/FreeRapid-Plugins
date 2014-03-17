package cz.vity.freerapid.plugins.services.hulu;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.tor.TorProxyClient;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 * @author tong2shot
 */
class HuluFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(HuluFileRunner.class.getName());

    private final static String SWF_URL = "http://download.hulu.com/huludesktop.swf";
    //private final static SwfVerificationHelper helper = new SwfVerificationHelper(SWF_URL);

    private final static String V_PARAM = "888324234";
    private final static String HMAC_KEY = "f6daaa397d51f568dd068709b0ce8e93293e078f7dfc3b40dd8c32d36d2b3ce1";
    private final static String DECRYPT_KEY = "d6dac049cc944519806ab9a1b5e29ccfe3e74dabb4fa42598a45c35d20abdd28";
    private final static String DECRYPT_IV = "27b9bedf75ccA2eC";
    private final static String SUBTITLE_DECRYPT_KEY = "4878b22e76379b55c962b18ddbc188d82299f8f52e3e698d0faf29a40ed64b21";
    private final static String SUBTITLE_DECRYPT_IV = "WA7hap7AGUkevuth";

    private final String sessionId = getSessionId();

    private String contentId;
    private boolean hasSubtitle = false;
    private HuluSettingsConfig config;

    private void setConfig() throws Exception {
        final HuluServiceImpl service = (HuluServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (isSubtitle()) return;
        final HttpMethod method = getGetMethod(fileURL);
        //Server sometimes sends a 404 response
        makeRedirectedRequest(method);
        checkProblems(getContentAsString());
        checkNameAndSize();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        if (isUserPage()) {
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
            return;
        }
        final Matcher matcher = getMatcherAgainstContent("window\\._preloadedFastStartVideo = ([^\r\n]+?\\})\\\\n");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name content not found");
        }
        final String data = matcher.group(1);
        try {
            final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            if (engine == null) {
                throw new RuntimeException("JavaScript engine not found");
            }
            engine.eval("var data = JSON.parse(\"" + data + "\")");

            final String show = engine.eval("data[\"show\"][\"name\"]").toString();
            final String title = engine.eval("data[\"title\"]").toString();
            String name;
            try {
                final int season = Integer.parseInt(engine.eval("data[\"season_number\"].toString()").toString());
                final int episode = Integer.parseInt(engine.eval("data[\"episode_number\"].toString()").toString());
                name = String.format("%s - S%02dE%02d - %s", show, season, episode, title);
            } catch (final Exception e) {
                //non episode
                name = title;
            }
            try {
                hasSubtitle = (engine.eval("data[\"has_captions\"]").toString().equals("true")); //has subtitle
            } catch (final Exception e) {
                //
            }
            httpFile.setFileName(name + ".flv");
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

            contentId = engine.eval("data[\"content_id\"]").toString();
        } catch (final Exception e) {
            logger.warning("data = " + data);
            throw new PluginImplementationException("Error getting file name", e);
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        if (isSubtitle()) {
            processSubtitle();
            return;
        }
        logger.info("Starting download in TASK " + fileURL);
        setConfig();
        login();

        HttpMethod method = getGetMethod(fileURL);
        makeRedirectedRequest(method);
        checkProblems(getContentAsString());
        checkNameAndSize();

        if (isUserPage()) {
            parseUserPage();
            return;
        }

        if (config.isDownloadSubtitles() && hasSubtitle) {
            //add filename to URL's tail so we can extract the filename later
            //http://www.hulu.com/captions.xml?content_id=40039219 -> original caption url
            //http://www.hulu.com/captions.xml?content_id=40039219/Jewel in the Palace - S01E01 - Episode 1 -> filename added at url's tail
            final String captionUrl = String.format("http://www.hulu.com/captions.xml?content_id=%s/%s", contentId, httpFile.getFileName().replace(".flv", ""));
            final List<URI> list = new LinkedList<URI>();
            try {
                list.add(new URI(new org.apache.commons.httpclient.URI(captionUrl, false, "UTF-8").toString()));
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        }

        final String contentSelectUrl = getContentSelectUrl(contentId);
        logger.info("contentSelectUrl = " + contentSelectUrl);

        method = getGetMethod(contentSelectUrl);
        final TorProxyClient torClient = TorProxyClient.forCountry("us", client, getPluginService().getPluginContext().getConfigurationStorageSupport());
        if (torClient.makeRequest(method)) {
            final String content = decryptContentSelect(getContentAsString());
            logger.info("Content select:\n" + content);
            try {
                checkProblems(content);
            } catch (final Exception e) {
                logger.warning("Content select:\n" + content);
                throw e;
            }

            final RtmpSession rtmpSession = getSession(getStream(getStreamMap(content)));
            rtmpSession.getConnectParams().put("pageUrl", SWF_URL);
            rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
            //helper.setSwfVerification(rtmpSession, client);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems(getContentAsString());
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems(final String content) throws ErrorDuringDownloadingException {
        if (content.contains("The page you were looking for doesn't exist")
                || content.contains("This content is unavailable for playback")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("Your Hulu Plus subscription allows you to watch one video at a time")) {
            throw new ServiceConnectionProblemException("Your Hulu Plus subscription allows you to watch only one video at a time");
        }
        if (content.contains("we noticed you are trying to access Hulu through")) {
            throw new NotRecoverableDownloadException("Hulu noticed that you are trying to access them through a proxy");
        }
    }

    private RtmpSession getSession(final Stream stream) {
        return new RtmpSession(stream.server, 1935, stream.app, stream.play, true);
    }

    private SortedMap<Integer, Stream> getStreamMap(String content) throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("<video server=\"(.+?)\" stream=\"(.+?)\" token=\"(.+?)\" system-bitrate=\"(\\d+?)\".*? height=\"(\\d+?)\".*? file-type=\"\\d+_(.+?)\".*? cdn=\"(?:darwin\\-)?(.+?)\"", content);
        final SortedMap<Integer, Stream> streamMap = new TreeMap<Integer, Stream>(); //k=video quality, v=stream, sorted by video quality ascending
        logger.info("Available streams : ");
        while (matcher.find()) {
            final String serverApp = matcher.group(1);
            final String play = matcher.group(2); //stream as play
            final String token = matcher.group(3);
            Matcher serverAppMatcher = PlugUtils.matcher("://(.+?)/(.+)", serverApp);
            if (!serverAppMatcher.find()) {
                throw new PluginImplementationException("Error parsing stream server");
            }
            final String server = serverAppMatcher.group(1);
            final String app = serverAppMatcher.group(2) + "?sessionid=" + sessionId + "&" + PlugUtils.replaceEntities(token);
            final int bitrate = Integer.parseInt(matcher.group(4));
            final int videoQuality = Integer.parseInt(matcher.group(5)); //height as video quality
            final String videoFormat = matcher.group(6);
            final String cdn = matcher.group(7);
            if (!cdn.equalsIgnoreCase("akamai") || !videoFormat.equalsIgnoreCase("h264")) { //ignore non-akamai, non-h264
                continue;
            }
            Stream stream = new Stream(server, app, play, bitrate, videoQuality, videoFormat, cdn);
            logger.info(stream.toString());
            streamMap.put(videoQuality, stream); //once we strip non-akamai, non-h264, it's safe to assume video quality as key
        }
        if (streamMap.isEmpty()) {
            throw new PluginImplementationException("No streams found");
        }
        return streamMap;
    }

    private Stream getStream(SortedMap<Integer, Stream> streamMap) {
        final int LOWER_QUALITY_PENALTY = 10;
        Stream selectedStream = null;
        int weight = Integer.MAX_VALUE;
        if (config.getVideoQuality() == VideoQuality.Highest) {
            selectedStream = streamMap.get(streamMap.lastKey());
        } else if (config.getVideoQuality() == VideoQuality.Lowest) {
            selectedStream = streamMap.get(streamMap.firstKey());
        } else {
            for (Map.Entry<Integer, Stream> streamEntry : streamMap.entrySet()) {
                Stream stream = streamEntry.getValue();
                int deltaQ = stream.videoQuality - config.getVideoQuality().getQuality();
                int tempWeight = (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ);
                if (tempWeight < weight) {
                    weight = tempWeight;
                    selectedStream = stream;
                }
            }
        }

        logger.info("Config settings : " + config);
        logger.info("Selected stream : " + selectedStream);
        return selectedStream;
    }

    private class Stream {
        private final String server;
        private final String play;
        private final String app;
        private final int bitrate;
        private final int videoQuality; //height as video quality
        private final String videoFormat; // Example : vp6, h264
        private final String cdn; //Example : akamai, limelight, level3

        public Stream(String server, String app, String play, int bitrate, int videoQuality, String videoFormat, String cdn) throws ErrorDuringDownloadingException {
            this.server = server;
            this.app = app;
            this.play = play;
            this.bitrate = bitrate;
            this.videoQuality = videoQuality;
            this.videoFormat = videoFormat;
            this.cdn = cdn;
        }

        @Override
        public String toString() {
            return "Stream{" +
                    "server='" + server + '\'' +
                    ", app='" + app + '\'' +
                    ", play='" + play + '\'' +
                    ", bitrate=" + bitrate +
                    ", videoQuality=" + videoQuality + 'p' +
                    ", videoformat=" + videoFormat +
                    ", cdn='" + cdn + '\'' +
                    '}';
        }
    }

    private static String getContentSelectUrl(final String cid) throws Exception {
        final Map<String, String> parameters = new LinkedHashMap<String, String>(); //preserve ordering
        parameters.put("video_id", cid);
        parameters.put("v", V_PARAM);
        parameters.put("ts", String.valueOf(System.currentTimeMillis()));
        parameters.put("np", "1");
        parameters.put("vp", "1");
        parameters.put("pp", "hulu");
        parameters.put("dp_id", "hulu");
        parameters.put("region", "US");
        parameters.put("language", "en");

        final StringBuilder sb = new StringBuilder("http://s.hulu.com/select?");
        for (final Map.Entry<String, String> e : parameters.entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append('&');
        }
        sb.append("bcs=").append(getBcs(parameters));
        return sb.toString();
    }

    private static String getBcs(final Map<String, String> parameters) throws Exception {
        final SortedMap<String, String> sortedParameters = new TreeMap<String, String>(parameters); //sorted by key
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, String> e : sortedParameters.entrySet()) {
            sb.append(e.getKey()).append(e.getValue());
        }
        final Mac mac = Mac.getInstance("HmacMD5");
        mac.init(new SecretKeySpec(HMAC_KEY.getBytes("UTF-8"), "HmacMD5"));
        return Hex.encodeHexString(mac.doFinal(sb.toString().getBytes("UTF-8")));
    }

    private static String decryptContentSelect(final String toDecrypt) throws Exception {
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(Hex.decodeHex(DECRYPT_KEY.toCharArray()), "AES"),
                new IvParameterSpec(DECRYPT_IV.getBytes("UTF-8")));
        return new String(cipher.doFinal(Hex.decodeHex(toDecrypt.toCharArray())), "UTF-8");
    }

    private static String getSessionId() {
        final byte[] bytes = new byte[16];
        new Random().nextBytes(bytes);
        return new String(Hex.encodeHex(bytes, false));
    }

    private boolean isUserPage() {
        return fileURL.contains("/profiles/");
    }

    private void parseUserPage() throws Exception {
        final Collection<URI> set = new LinkedHashSet<URI>();
        for (int page = 1; ; page++) {
            final HttpMethod method = getMethodBuilder().setAction(fileURL).setParameter("page", String.valueOf(page)).toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final int previousSize = set.size();
            final Matcher matcher = getMatcherAgainstContent("<a href=\"(http://www\\.hulu\\.com/watch/[^\"]+?)\" beaconid=\"");
            while (matcher.find()) {
                try {
                    set.add(new URI(matcher.group(1)));
                } catch (final URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
            if (set.size() <= previousSize) {
                break;
            }
        }
        if (set.isEmpty()) {
            throw new NotRecoverableDownloadException("No videos found");
        }
        final List<URI> list = new ArrayList<URI>(set);
        // Hulu returns the videos in descending date order, which is a bit illogical
        Collections.reverse(list);
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        logger.info(set.size() + " videos added");
        httpFile.getProperties().put("removeCompleted", true);
    }

    private boolean login() throws Exception {
        logger.info("Entering login subroutine...");
        if (config.getUsername() == null || config.getUsername().isEmpty()) {
            logger.info("No account data set, skipping login");
            return false;
        }
        setFileStreamContentTypes(new String[0], new String[]{"application/x-www-form-urlencoded"});
        final HttpMethod method = getMethodBuilder()
                .setAction("https://secure.hulu.com/account/authenticate")
                .setParameter("login", config.getUsername())
                .setParameter("password", config.getPassword())
                .setParameter("sli", "1")
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException("Error posting login info");
        }
        if (!getContentAsString().contains("ok=1")) {
            throw new BadLoginException("Invalid Hulu account login information");
        }
        return true;
    }

    private boolean isSubtitle() {
        return fileURL.matches("http://(www\\.)?hulu\\.com/captions\\.xml\\?content_id=\\d+/.+");
    }

    private void processSubtitle() throws Exception {
        //http://www.hulu.com/captions.xml?content_id=40039219 -> original caption url
        //http://www.hulu.com/captions.xml?content_id=40039219/Jewel in the Palace - S01E01 - Episode 1 -> filename added at url's tail
        httpFile.setFileName(URLDecoder.decode(fileURL.substring(fileURL.lastIndexOf("/") + 1), "UTF-8"));
        fileURL = fileURL.substring(0, fileURL.lastIndexOf("/")); //remove "/"+filename
        GetMethod method = getGetMethod(fileURL);
        setFileStreamContentTypes(new String[0], new String[]{"application/xml", "application/smil"});
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException("Error downloading subtitle");
        }
        //<?xml version="1.0" encoding="utf-8"?><transcripts><en>http://assets.huluim.com/captions/219/40039219_US_ko_en.smi</en></transcripts>
        Matcher matcher = getMatcherAgainstContent("<transcripts>\\s*<en>\\s*(.+?)\\s*</en>\\s*</transcripts>");
        if (!matcher.find()) {
            logger.warning(getContentAsString());
            throw new PluginImplementationException("Subtitle not found");
        }
        final String captionUrl = matcher.group(1);
        final String extension = captionUrl.substring(captionUrl.lastIndexOf("."));
        httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(httpFile.getFileName() + extension, "_"));
        method = getGetMethod(captionUrl);
        setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
        if (extension.equals(".smi")) {
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error downloading subtitle-2");
            }
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Hex.decodeHex(SUBTITLE_DECRYPT_KEY.toCharArray()), "AES"), new IvParameterSpec(SUBTITLE_DECRYPT_IV.getBytes()));
            final StringBuilder subtitleSb = new StringBuilder(100);
            subtitleSb.append("<SAMI><BODY>");
            matcher = Pattern.compile("<SYNC Encrypted=\"true\" start=\"(\\d+)\">(.+?)</SYNC>", Pattern.CASE_INSENSITIVE).matcher(getContentAsString());
            while (matcher.find()) {
                final String plainText = PlugUtils.replaceEntities(PlugUtils.unescapeHtml(new String(cipher.doFinal(Hex.decodeHex(matcher.group(2).toCharArray())), "UTF-8")));
                subtitleSb.append(String.format("<SYNC start=%s>%s</SYNC>\n", matcher.group(1), plainText));
            }
            subtitleSb.append("</BODY></SAMI>");
            //logger.info(subtitleSb.toString());
            final byte[] subtitle = subtitleSb.toString().getBytes("UTF-8");
            httpFile.setFileSize(subtitle.length);
            try {
                downloadTask.saveToFile(new ByteArrayInputStream(subtitle));
            } catch (final Exception e) {
                LogUtils.processException(logger, e);
                throw new PluginImplementationException("Error saving subtitle", e);
            }
        } else { //non .smi subtitle, haven't tested, couldn't find sample
            if (!tryDownloadAndSaveFile(method)) {
                throw new PluginImplementationException("Error saving subtitle");
            }
        }
    }

}