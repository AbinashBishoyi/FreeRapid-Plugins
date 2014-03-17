package cz.vity.freerapid.plugins.services.hulu;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.tunlr.Tunlr;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import cz.vity.freerapid.utilities.crypto.Cipher;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

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

    private final String sessionId = getSessionId();

    private String contentId;
    private boolean hasCaption = false;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (isCaptionUrl()) return;
        fileURL = fileURL.replaceFirst("//(www\\.)?hulu\\.com", "//new.hulu.com");
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
        Matcher matcher = getMatcherAgainstContent("new Hulu\\.Models\\.Video\\((.+?)\\);");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name content not found");
        }
        try {
            final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            if (engine == null) {
                throw new RuntimeException("JavaScript engine not found");
            }
            engine.eval("var data = " + matcher.group(1));

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
                hasCaption = (engine.eval("data[\"has_captions\"]").toString().equals("true")); //has subtitle
            } catch (final Exception e) {
                //
            }
            httpFile.setFileName(name + ".flv");
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

            contentId = engine.eval("data[\"content_id\"]").toString();
        } catch (final Exception e) {
            logger.warning("data = " + matcher.group(1));
            throw new PluginImplementationException("Error getting file name", e);
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        if (isCaptionUrl()) {
            processCaption();
            return;
        }
        fileURL = fileURL.replaceFirst("//(www\\.)?hulu\\.com", "//new.hulu.com");
        logger.info("Starting download in TASK " + fileURL);
        login();

        HttpMethod method = getGetMethod(fileURL);
        makeRedirectedRequest(method);
        checkProblems(getContentAsString());
        checkNameAndSize();

        if (isUserPage()) {
            parseUserPage();
            return;
        }

        if (hasCaption) {
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
        if (!client.getSettings().isProxySet()) {
            Tunlr.setupMethod(method);
        }
        if (makeRedirectedRequest(method)) {
            final String content = decryptContentSelect(getContentAsString());
            logger.info("Content select:\n" + content);
            checkProblems(content);

            final RtmpSession rtmpSession = getStream(content);
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
        if (content.contains("we noticed you are trying to access Hulu through")) {
            throw new NotRecoverableDownloadException("Hulu noticed that you are trying to access them through a proxy");
        }
    }

    private RtmpSession getStream(final String content) throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("<video server=\"(.+?)\" stream=\"(.+?)\" token=\"(.+?)\" system-bitrate=\"(\\d+?)\".*? cdn=\"(.+?)\"", content);
        final List<Stream> list = new LinkedList<Stream>();
        while (matcher.find()) {
            final String cdn = matcher.group(5);
            if ("level3".equals(cdn)) {
                logger.info("Ignoring stream served by Level3");
            } else {
                list.add(new Stream(matcher.group(1), matcher.group(2), matcher.group(3), Integer.parseInt(matcher.group(4)), cdn));
            }
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No streams found");
        }
        return Collections.max(list).getSession();
    }

    private class Stream implements Comparable<Stream> {
        private final String server;
        private final String play;
        private final String app;
        private final int bitrate;
        private final String cdn;

        public Stream(String server, String stream, String token, int bitrate, String cdn) throws ErrorDuringDownloadingException {
            Matcher matcher = PlugUtils.matcher("://(.+?)/(.+)", server);
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing stream server");
            }
            server = matcher.group(1);
            token = matcher.group(2) + "?sessionid=" + sessionId + "&" + PlugUtils.replaceEntities(token);
            this.server = server;
            this.play = stream;
            this.app = token;
            this.bitrate = bitrate;
            this.cdn = cdn;
            logger.info("Found stream: " + this);
        }

        public RtmpSession getSession() {
            logger.info("Downloading stream: " + this);
            return new RtmpSession(server, 1935, app, play, true);
        }

        @Override
        public int compareTo(Stream that) {
            final int i = Integer.valueOf(this.bitrate).compareTo(that.bitrate);
            //Prefer akamai streams as they are often faster and allow non-US IPs
            if (i == 0) {
                final boolean thisAkamai = "akamai".equals(this.cdn);
                final boolean thatAkamai = "akamai".equals(that.cdn);
                if (thisAkamai == thatAkamai) {
                    return 0;
                } else if (thisAkamai) {
                    return 1;
                } else {
                    return -1;
                }
            }
            return i;
        }

        @Override
        public String toString() {
            return "Stream{" +
                    "server='" + server + '\'' +
                    ", play='" + play + '\'' +
                    ", app='" + app + '\'' +
                    ", bitrate=" + bitrate +
                    ", cdn='" + cdn + '\'' +
                    '}';
        }
    }

    private static String getContentSelectUrl(final String cid) throws Exception {
        final Parameters parameters = new Parameters()
                .add("video_id", cid)
                .add("v", V_PARAM)
                .add("ts", String.valueOf(System.currentTimeMillis()))
                .add("np", "1")
                .add("vp", "1")
                .add("pp", "hulu")
                .add("dp_id", "hulu")
                .add("region", "US")
                .add("language", "en");
        final StringBuilder sb = new StringBuilder("http://s.hulu.com/select?");
        for (final Map.Entry<String, String> e : parameters) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append('&');
        }
        sb.append("bcs=").append(getBcs(parameters));
        return sb.toString();
    }

    private static String getBcs(final Parameters parameters) throws Exception {
        parameters.sort();
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, String> e : parameters) {
            sb.append(e.getKey()).append(e.getValue());
        }
        final Mac mac = Mac.getInstance("HmacMD5");
        mac.init(new SecretKeySpec(HMAC_KEY.getBytes("UTF-8"), "HmacMD5"));
        return Hex.encodeHexString(mac.doFinal(sb.toString().getBytes("UTF-8")));
    }

    private static class Parameters implements Iterable<Map.Entry<String, String>> {
        private final List<Map.Entry<String, String>> parameters = new LinkedList<Map.Entry<String, String>>();

        public Parameters add(final String key, final String value) {
            parameters.add(new AbstractMap.SimpleImmutableEntry<String, String>(key, value));
            return this;
        }

        public void sort() {
            Collections.sort(parameters, new Comparator<Map.Entry<String, String>>() {
                @Override
                public int compare(final Map.Entry<String, String> o1, final Map.Entry<String, String> o2) {
                    return o1.getKey().compareTo(o2.getKey());
                }
            });
        }

        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            return parameters.iterator();
        }
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
        final PremiumAccount pa = ((HuluServiceImpl) getPluginService()).getConfig();
        if (pa == null || !pa.isSet()) {
            logger.info("No account data set, skipping login");
            return false;
        }
        setFileStreamContentTypes(new String[0], new String[]{"application/x-www-form-urlencoded"});
        final HttpMethod method = getMethodBuilder()
                .setAction("https://secure.hulu.com/account/authenticate")
                .setParameter("login", pa.getUsername())
                .setParameter("password", pa.getPassword())
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

    private boolean isCaptionUrl() {
        return fileURL.matches("http://(www\\.)?hulu\\.com/captions\\.xml\\?content_id=\\d+/.+");
    }

    private void processCaption() throws Exception {
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
        httpFile.setFileName(httpFile.getFileName() + extension);
        method = getGetMethod(captionUrl);
        setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
        if (extension.equals(".smi")) {
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error downloading subtitle-2");
            }
            byte[] decryptKey = Hex.decodeHex("625298045c1db17fe3489ba7f1eba2f208b3d2df041443a72585038e24fc610b".toCharArray());
            byte[] decrypytIV = "V@6i`q6@FTjdwtui".getBytes("UTF-8");
            for (int i = 0; i < decryptKey.length; i++) {
                decryptKey[i] = (byte) (decryptKey[i] ^ 42);
            }
            for (int i = 0; i < decrypytIV.length; i++) {
                decrypytIV[i] = (byte) (decrypytIV[i] ^ 1);
            }
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptKey, "AES"), new IvParameterSpec(decrypytIV));
            final StringBuilder subtitleSb = new StringBuilder(100);
            subtitleSb.append("<SAMI><BODY>");
            matcher = Pattern.compile("<SYNC Encrypted=\"true\" start=\"(\\d+)\">(.+?)</SYNC>", Pattern.CASE_INSENSITIVE).matcher(getContentAsString());
            while (matcher.find()) {
                final String plainText = PlugUtils.replaceEntities(PlugUtils.unescapeHtml(new String(cipher.doFinal(Hex.decodeHex(matcher.group(2).toCharArray())), "UTF-8")));
                subtitleSb.append(String.format("<SYNC start=%s>%s</SYNC>\n", matcher.group(1), plainText));
            }
            subtitleSb.append("</BODY></SAMI>");
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