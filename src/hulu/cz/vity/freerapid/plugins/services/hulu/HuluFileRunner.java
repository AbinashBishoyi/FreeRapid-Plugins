package cz.vity.freerapid.plugins.services.hulu;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.cryptography.CryptographySupport;
import cz.vity.freerapid.plugins.services.cryptography.Engine;
import cz.vity.freerapid.plugins.services.cryptography.Mode;
import cz.vity.freerapid.plugins.services.cryptography.Padding;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.interfaces.FileStreamRecognizer;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class HuluFileRunner extends AbstractRtmpRunner implements FileStreamRecognizer {
    private final static Logger logger = Logger.getLogger(HuluFileRunner.class.getName());

    private final static String CID_KEY = "48555bbbe9f41981df49895f44c83993a09334d02d17e7a76b237d04c084e342";
    private final static String EID_CONST = "MAZxpK3WwazfARjIpSXKQ9cmg9nPe5wIOOfKuBIfz7bNdat6gQKHj69ZWNWNVB1";
    private final static String MD5_SALT = "yumUsWUfrAPraRaNe2ru2exAXEfaP6Nugubepreb68REt7daS79fase9haqar9sa";
    private final static String DECRYPT_KEY = "625298045c1db17fe3489ba7f1eba2f208b3d2df041443a72585038e24fc610b";
    private final static String DECRYPT_IV = "V@6i`q6@FTjdwtui";

    private final String sessionId = getSessionId();

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
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
            client.getHTTPClient().getParams().setParameter(DownloadClientConsts.FILE_STREAM_RECOGNIZER, this);
            final String cid = PlugUtils.getStringBetween(getContentAsString(), "\"content_id\", \"", "\"");
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
                    final Matcher matcher = PlugUtils.matcher("<video server=\"(.+?)\" stream=\"(.+?)\" token=\"(.+?)\" system-bitrate=\"(\\d+?)\"", content);
                    final List<Stream> list = new ArrayList<Stream>();
                    while (matcher.find()) {
                        list.add(new Stream(matcher.group(1), matcher.group(2), matcher.group(3), Integer.parseInt(matcher.group(4))));
                    }
                    if (list.isEmpty()) throw new PluginImplementationException("No streams found");
                    Collections.sort(list);
                    final Stream stream = list.get(0);
                    final RtmpSession rtmpSession = new RtmpSession(stream.getServer(), 80, stream.getApp(), stream.getPlay(), true);
                    rtmpSession.getConnectParams().put("pageUrl", fileURL);
                    rtmpSession.getConnectParams().put("swfUrl", "http://www.hulu.com/site-player/playback.swf");
                    //SWF verification is not necessary at the moment
                    //rtmpSession.initSwfVerification("C:\\Users\\Administrator\\Desktop\\playback.swf");
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

    @Override
    public boolean isStream(HttpMethod method, boolean showWarnings) {
        final Header h = method.getResponseHeader("Content-Type");
        if (h == null) return false;
        final String contentType = h.getValue().toLowerCase(Locale.ENGLISH);
        return (!contentType.startsWith("text/") && !contentType.contains("xml"));
    }

    private class Stream implements Comparable<Stream> {
        private final String server;
        private final String play;
        private final String app;
        private final int bitrate;

        public Stream(String server, String stream, String token, int bitrate) throws Exception {
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

    private static byte[] md5(final String data) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(data.getBytes("UTF-8"));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            LogUtils.processException(logger, e);
        } catch (UnsupportedEncodingException e) {
            LogUtils.processException(logger, e);
        }
        return null;
    }

    private static String getContentId(final String text) {
        Thing _local3 = R.AK();
        R.sdk(CID_KEY, CID_KEY.length() * 4, _local3);
        String _local2 = R.e(text, _local3);
        _local2 = R.h2s(_local2);
        return _local2.split("~")[0];
    }

    public static String parseContentId(final String _arg1) {
        if (_arg1.charAt(0) == 'm') {
            return new BigInteger(_arg1.substring(1), 36).xor(BigInteger.valueOf(3735928559L)).toString();
        }
        return _arg1;
    }

    private static String cidToEid(final String cid) throws Exception {
        return new String(Base64.encodeBase64(md5(cid + EID_CONST)), "UTF-8").replace('+', '-').replace('/', '_').replace("=", "");
    }

    private static String getContentSelectUrl(final String pid) {
        final byte[] hash = md5(pid + MD5_SALT);
        final StringBuilder sb = new StringBuilder(32);
        for (final byte b : hash) {
            final String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return "http://s.hulu.com/select.ashx?pid=" + pid + "&auth=" + sb.toString() + "&v=713434170&np=1&pp=hulu&dp_id=hulu&cb=" + new Random().nextInt(1000);
    }

    private static String decryptContentSelect(final String toDecrypt) throws Exception {
        final byte[] key = Hex.decodeHex(DECRYPT_KEY.toCharArray());
        final byte[] iv = DECRYPT_IV.getBytes("UTF-8");
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (key[i] ^ 42);
        }
        for (int i = 0; i < iv.length; i++) {
            iv[i] = (byte) (iv[i] ^ 1);
        }
        return new CryptographySupport().setEngine(Engine.AES).setMode(Mode.CBC).setPadding(Padding.PKCS7).setKey(key).setIV(iv).decrypt(toDecrypt);
    }

    private static String getSessionId() {
        final byte[] hash = md5(String.valueOf(System.currentTimeMillis() + new Random().nextInt()));
        final StringBuilder sb = new StringBuilder(32);
        for (final byte b : hash) {
            final String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString().toUpperCase(Locale.ENGLISH);
    }

}