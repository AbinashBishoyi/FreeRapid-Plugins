package cz.vity.freerapid.plugins.services.hulu;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import cz.vity.freerapid.utilities.crypto.Cipher;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpMethod;

import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
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

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        //Server sometimes sends a 404 response
        makeRedirectedRequest(method);
        checkProblems(getContentAsString());
        checkNameAndSize();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("<title>Hulu \\- (.+?)(?: \\- Watch|</title>)");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        String name = matcher.group(1).replace(": ", " - ");
        if (!isUserPage()) {
            matcher = getMatcherAgainstContent("Season (\\d+) [^<>]*?Ep\\. (\\d+)");
            if (matcher.find()) {
                final String[] s = name.split(" \\- ", 2);
                if (s.length >= 2) {
                    final int season = Integer.parseInt(matcher.group(1));
                    final int episode = Integer.parseInt(matcher.group(2));
                    name = String.format("%s - S%02dE%02d - %s", s[0], season, episode, s[1]);
                }
            }
            httpFile.setFileName(name + ".flv");
        } else {
            httpFile.setFileName(name);
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        HttpMethod method = getGetMethod(fileURL);
        makeRedirectedRequest(method);
        checkProblems(getContentAsString());
        checkNameAndSize();

        if (isUserPage()) {
            parseUserPage();
            return;
        }

        final String cid = PlugUtils.getStringBetween(getContentAsString(), "content_id =", ";");
        final String contentSelectUrl = getContentSelectUrl(cid);
        logger.info("contentSelectUrl = " + contentSelectUrl);

        method = getGetMethod(contentSelectUrl);
        if (makeRedirectedRequest(method)) {
            final String content = decryptContentSelect(getContentAsString());
            logger.info("Content select:\n" + content);

            checkProblems(content);
            geoCheck(content);

            final Stream stream = getStream(content);
            final RtmpSession rtmpSession = new RtmpSession(stream.getServer(), 80, stream.getApp(), stream.getPlay(), true);
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

    private void geoCheck(final String content) throws Exception {
        if (!client.getSettings().isProxySet()) {
            // Do not perform geocheck if using a proxy.
            // The geocheck server detects proxies better than the stream server,
            // which may cause issues.
            if (content.contains("allowInternational=\"false\"")) {
                logger.info("Performing geocheck");
                final HttpMethod method = getGetMethod("http://releasegeo.hulu.com/geoCheck");
                if (makeRedirectedRequest(method)) {
                    if (getContentAsString().contains("not-valid")) {
                        throw new NotRecoverableDownloadException("This video can only be streamed in the US");
                    }
                } else {
                    checkProblems(getContentAsString());
                    throw new ServiceConnectionProblemException();
                }
            }
        }
    }

    private Stream getStream(final String content) throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("<video server=\"(.+?)\" stream=\"(.+?)\" token=\"(.+?)\" system-bitrate=\"(\\d+?)\"", content);
        final List<Stream> list = new LinkedList<Stream>();
        while (matcher.find()) {
            list.add(new Stream(matcher.group(1), matcher.group(2), matcher.group(3), Integer.parseInt(matcher.group(4))));
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No streams found");
        }
        return Collections.min(list);
    }

    private class Stream implements Comparable<Stream> {
        private final String server;
        private final String play;
        private final String app;
        private final int bitrate;

        public Stream(String server, String stream, String token, int bitrate) throws ErrorDuringDownloadingException {
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
            logger.info("server = " + this.server);
            logger.info("play = " + this.play);
            logger.info("app = " + this.app);
            logger.info("bitrate = " + this.bitrate);
        }

        public String getServer() {
            return server;
        }

        public String getPlay() {
            return play;
        }

        public String getApp() {
            return app;
        }

        @Override
        public int compareTo(Stream that) {
            return Integer.valueOf(that.bitrate).compareTo(this.bitrate);
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
        return new String(Hex.encodeHex(mac.doFinal(sb.toString().getBytes("UTF-8"))));
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
        return DigestUtils.md5Hex(String.valueOf(System.nanoTime())).toUpperCase(Locale.ENGLISH);
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

}