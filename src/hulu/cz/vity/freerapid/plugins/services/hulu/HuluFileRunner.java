package cz.vity.freerapid.plugins.services.hulu;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.crypto.Cipher;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpMethod;

import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
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

    private final static String HMAC_KEY = "f6daaa397d51f568dd068709b0ce8e93293e078f7dfc3b40dd8c32d36d2b3ce1";
    private final static String DECRYPT_KEY = "40A757F83B2348A7B5F7F41790FDFFA02F72FC8FFD844BA6B28FD5DFD8CFC82F";
    private final static String DECRYPT_IV = "NnemTiVU0UA5jVl0";

    private final String sessionId = getSessionId();

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("<title>Hulu \\- (.+?)(?: \\- Watch|</title>)");
        if (!matcher.find()) throw new PluginImplementationException("File name not found");
        String name = matcher.group(1).replace(": ", " - ");
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
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final String cid = PlugUtils.getStringBetween(getContentAsString(), "\"content_id\", ", ")");
            final String contentSelectUrl = getContentSelectUrl(cid);
            logger.info("contentSelectUrl = " + contentSelectUrl);
            method = getGetMethod(contentSelectUrl);
            if (makeRedirectedRequest(method)) {
                final String content = decryptContentSelect(getContentAsString());
                logger.info("Content select:\n" + content);
                if (content.contains("we noticed you are trying to access Hulu through")) {
                    throw new NotRecoverableDownloadException("Hulu noticed that you are trying to access them through a proxy");
                }
                geoCheck(content);
                final Stream stream = getStream(content);
                final RtmpSession rtmpSession = new RtmpSession(stream.getServer(), 80, stream.getApp(), stream.getPlay(), true);
                rtmpSession.getConnectParams().put("pageUrl", SWF_URL);
                rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
                //helper.setSwfVerification(rtmpSession, client);
                tryDownloadAndSaveFile(rtmpSession);
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("The page you were looking for doesn't exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
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
                    checkProblems();
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
        if (list.isEmpty()) throw new PluginImplementationException("No streams found");
        return Collections.min(list);
    }

    private class Stream implements Comparable<Stream> {
        private final String server;
        private final String play;
        private final String app;
        private final int bitrate;

        public Stream(String server, String stream, String token, int bitrate) throws ErrorDuringDownloadingException {
            Matcher matcher = PlugUtils.matcher("://(.+?)/(.+)", server);
            if (!matcher.find()) throw new PluginImplementationException("Error parsing stream server");
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
                .add("v", "850037518")
                .add("ts", String.valueOf(System.currentTimeMillis()))
                .add("np", "1")
                .add("vp", "1")
                .add("pp", "Desktop")
                .add("dp_id", "Hulu")
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
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Hex.decodeHex(DECRYPT_KEY.toCharArray()), "AES"), new IvParameterSpec(DECRYPT_IV.getBytes("UTF-8")));
        return new String(cipher.doFinal(Hex.decodeHex(toDecrypt.toCharArray())), "UTF-8");
    }

    private static String getSessionId() {
        return DigestUtils.md5Hex(String.valueOf(System.currentTimeMillis() + new Random().nextInt())).toUpperCase(Locale.ENGLISH);
    }

}