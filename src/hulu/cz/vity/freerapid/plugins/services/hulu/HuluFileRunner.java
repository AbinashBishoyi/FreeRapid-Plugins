package cz.vity.freerapid.plugins.services.hulu;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.crypto.Cipher;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpMethod;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
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

    private final static String SWF_URL = "http://www.hulu.com/site-player/82388/player.swf?cb=82388";
    //private final static SwfVerificationHelper helper = new SwfVerificationHelper(SWF_URL);

    private final static String CID_KEY = "48555bbbe9f41981df49895f44c83993a09334d02d17e7a76b237d04c084e342";
    private final static String EID_CONST = "MAZxpK3WwazfARjIpSXKQ9cmg9nPe5wIOOfKuBIfz7bNdat6gQKHj69ZWNWNVB1";
    private final static String MD5_SALT = "yumUsWUfrAPraRaNe2ru2exAXEfaP6Nugubepreb68REt7daS79fase9haqar9sa";
    private final static String DECRYPT_KEY_STR = "625298045c1db17fe3489ba7f1eba2f208b3d2df041443a72585038e24fc610b";
    private final static String DECRYPT_IV_STR = "V@6i`q6@FTjdwtui";
    private final static byte[] DECRYPT_KEY;
    private final static byte[] DECRYPT_IV;

    static {
        try {
            final byte[] key = Hex.decodeHex(DECRYPT_KEY_STR.toCharArray());
            final byte[] iv = DECRYPT_IV_STR.getBytes("UTF-8");
            for (int i = 0; i < key.length; i++) {
                key[i] = (byte) (key[i] ^ 42);
            }
            for (int i = 0; i < iv.length; i++) {
                iv[i] = (byte) (iv[i] ^ 1);
            }
            DECRYPT_KEY = key;
            DECRYPT_IV = iv;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
            final String eidUrl = "http://r.hulu.com/videos?eid=" + cidToEid(parseContentId(getContentId(cid)));
            logger.info("eidUrl = " + eidUrl);
            method = getGetMethod(eidUrl);
            if (makeRedirectedRequest(method)) {
                final String pid = Security.decrypt(PlugUtils.getStringBetween(getContentAsString(), "<pid>", "</pid>"));
                logger.info("pid = " + pid);
                final String contentSelectUrl = getContentSelectUrl(pid);
                logger.info("contentSelectUrl = " + contentSelectUrl);
                method = getGetMethod(contentSelectUrl);
                if (makeRedirectedRequest(method)) {
                    final String content = decryptContentSelect(getContentAsString());
                    logger.info("Content select:\n" + content);
                    if (content.contains("we noticed you are trying to access Hulu through")) {
                        throw new NotRecoverableDownloadException("Hulu noticed that you are trying to access them through a proxy");
                    }
                    if (!client.getSettings().isProxySet()) {
                        // Do not perform geocheck if using a proxy.
                        // The geocheck server detects proxies better than the stream server,
                        // which may cause issues.
                        if (content.contains("allowInternational=\"false\"")) {
                            logger.info("Performing geocheck");
                            method = getGetMethod("http://releasegeo.hulu.com/geoCheck");
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
                    final Stream stream = getStream(content);
                    final RtmpSession rtmpSession = new RtmpSession(stream.getServer(), 80, stream.getApp(), stream.getPlay(), true);
                    rtmpSession.getConnectParams().put("pageUrl", fileURL);
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

    private static String getContentId(final String text) {
        if (text.length() > 12) {
            Thing _local3 = R.AK();
            R.sdk(CID_KEY, CID_KEY.length() * 4, _local3);
            String _local2 = R.e(text, _local3);
            _local2 = R.h2s(_local2);
            return _local2.split("~")[0];
        } else {
            return text;
        }
    }

    public static String parseContentId(final String _arg1) {
        if (_arg1.charAt(0) == 'm') {
            return new BigInteger(_arg1.substring(1), 36).xor(BigInteger.valueOf(3735928559L)).toString();
        }
        return _arg1;
    }

    private static String cidToEid(final String cid) throws Exception {
        return new String(Base64.encodeBase64(DigestUtils.md5(cid + EID_CONST), false, true), "UTF-8");
    }

    private static String getContentSelectUrl(final String pid) {
        final String auth = DigestUtils.md5Hex(pid + MD5_SALT);
        return "http://s.hulu.com/select.ashx?pid=" + pid + "&auth=" + auth + "&v=713434170&np=1&pp=hulu&dp_id=hulu&cb=" + new Random().nextInt(1000);
    }

    private static String decryptContentSelect(final String toDecrypt) throws Exception {
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(DECRYPT_KEY, "AES"), new IvParameterSpec(DECRYPT_IV));
        return new String(cipher.doFinal(Hex.decodeHex(toDecrypt.toCharArray())), "UTF-8");
    }

    private static String getSessionId() {
        return DigestUtils.md5Hex(String.valueOf(System.currentTimeMillis() + new Random().nextInt())).toUpperCase(Locale.ENGLISH);
    }

}