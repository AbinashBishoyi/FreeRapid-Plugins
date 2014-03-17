package cz.vity.freerapid.plugins.services.channel5;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.rtmp.SwfVerificationHelper;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class Channel5FileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(Channel5FileRunner.class.getName());
    private final static String SWF_URL = "http://admin.brightcove.com/viewer/us20130204.1211/connection/ExternalConnection_2.swf";
    private final static SwfVerificationHelper helper = new SwfVerificationHelper(SWF_URL);

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("<h[12]><span class=\"sifr_white\">(.+?)</span></h[12]>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found (1)");
        }
        String name = matcher.group(1);
        matcher = getMatcherAgainstContent("<h3 class=\"episode_header\"><span class=\"[^\"]+?\">(?:(?:Season|Series) (\\d+))?(?: \\- )?(?:Episode (\\d+))?(?:: )?(.+?)?</span></h3>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found (2)");
        }
        final String seasonNum = matcher.group(1);
        final String episodeNum = matcher.group(2);
        if (seasonNum != null && episodeNum != null) {
            final int seasonNumI = Integer.parseInt(seasonNum);
            final int episodeNumI = Integer.parseInt(episodeNum);
            name += String.format(" - S%02dE%02d", seasonNumI, episodeNumI);
        } else if (seasonNum != null) {
            final int seasonNumI = Integer.parseInt(seasonNum);
            name += String.format(" - S%02d", seasonNumI);
        } else if (episodeNum != null) {
            final int episodeNumI = Integer.parseInt(episodeNum);
            name += String.format(" - E%02d", episodeNumI);
        }
        final String episodeName = matcher.group(3);
        if (episodeName != null) {
            name += " - " + episodeName;
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
            final String pubId = PlugUtils.getStringBetween(getContentAsString(), "publisherID=", "&");
            setPageEncoding("ISO-8859-1");
            setFileStreamContentTypes(new String[0], new String[]{"application/x-amf"});
            method = getPostMethod("http://c.brightcove.com/services/messagebroker/amf?playerId=1707001745001");
            ((PostMethod) method).setRequestEntity(new ByteArrayRequestEntity(getAmfPostData(), "application/x-amf"));
            //bypassing the geocheck here is useless as the rtmp server also checks it
            //method.setRequestHeader("X-Forwarded-For", "212.169.3.171");
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            if (getContentAsString().contains("The video you are trying to watch cannot be viewed from your current country or location")) {
                throw new NotRecoverableDownloadException("The video you are trying to watch cannot be viewed from your current country or location");
            }
            final RtmpSession rtmpSession = getRtmpSession(pubId);
            rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
            rtmpSession.getConnectParams().put("pageUrl", fileURL);
            helper.setSwfVerification(rtmpSession, client);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("this page does not exist")
                || getContentAsString().contains("this episode is no longer available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private byte[] getAmfPostData() throws Exception {
        final String videoId = PlugUtils.getStringBetween(getContentAsString(), "videoPlayer=ref:", "&");
        if (videoId.length() != 11) {
            throw new PluginImplementationException("Unexpected video ID length");
        }
        final byte[] data = Base64.decodeBase64("AAMAAAABAEZjb20uYnJpZ2h0Y292ZS5leHBlcmllbmNlLkV4cGVyaWVuY2VSdW50aW1lRmFjYWRlLmdldERhdGFGb3JFeHBlcmllbmNlAAIvMVhYWFgKAAAAAgIAKDUwNDEwYmM2OTA1MmRhODBiMzNmOGQwZTUyNTZjZDE2ZDM2YjFiZDURCmNjY29tLmJyaWdodGNvdmUuZXhwZXJpZW5jZS5WaWV3ZXJFeHBlcmllbmNlUmVxdWVzdCFjb250ZW50T3ZlcnJpZGVzGWV4cGVyaWVuY2VJZAdVUkwZZGVsaXZlcnlUeXBlEVRUTFRva2VuE3BsYXllcktleQkDAQqBA1Njb20uYnJpZ2h0Y292ZS5leHBlcmllbmNlLkNvbnRlbnRPdmVycmlkZRtjb250ZW50UmVmSWRzE2NvbnRlbnRJZBdjb250ZW50VHlwZRtmZWF0dXJlZFJlZklkFWNvbnRlbnRJZHMNdGFyZ2V0FWZlYXR1cmVkSWQZY29udGVudFJlZklkAQV/////4AAAAAQAAQEGF3ZpZGVvUGxheWVyBX/////gAAAABhdYWFhYWFhYWFhYWAVCeNcTuOaQAAY=");
        final byte[] data2 = Base64.decodeBase64("BX/////gAAAABgEGAQ==");

        ByteBuffer.wrap(data, 431, 11).put(videoId.getBytes("UTF-8"));

        final ByteBuffer urlBuf = ByteBuffer.allocate(fileURL.length() + 20);
        putAmf3String(urlBuf, fileURL);
        urlBuf.flip();

        ByteBuffer.wrap(data, 82, 4).putInt(379 + urlBuf.limit());

        final ByteBuffer postData = ByteBuffer.wrap(new byte[data.length + urlBuf.limit() + data2.length]);
        postData.put(data);
        postData.put(urlBuf);
        postData.put(data2);
        return postData.array();
    }

    private static void putAmf3String(final ByteBuffer buf, final String s) throws Exception {
        final byte[] b = s.getBytes("UTF-8");
        putAmf3Integer(buf, (b.length << 1) | 1);
        buf.put(b);
    }

    private static void putAmf3Integer(final ByteBuffer buf, long value) throws Exception {
        if ((value >= -268435456) && (value <= 268435455)) {
            value &= 536870911;
        }
        if (value < 128) {
            buf.put((byte) value);
        } else if (value < 16384) {
            buf.put((byte) (((value >> 7) & 0x7F) | 0x80));
            buf.put((byte) (value & 0x7F));
        } else if (value < 2097152) {
            buf.put((byte) (((value >> 14) & 0x7F) | 0x80));
            buf.put((byte) (((value >> 7) & 0x7F) | 0x80));
            buf.put((byte) (value & 0x7F));
        } else if (value < 1073741824) {
            buf.put((byte) (((value >> 22) & 0x7F) | 0x80));
            buf.put((byte) (((value >> 15) & 0x7F) | 0x80));
            buf.put((byte) (((value >> 8) & 0x7F) | 0x80));
            buf.put((byte) (value & 0xFF));
        } else {
            throw new PluginImplementationException("AMF3 integer out of range: " + value);
        }
    }

    private RtmpSession getRtmpSession(final String pubId) throws Exception {
        Matcher matcher = getMatcherAgainstContent("mediaDTO\n\u0001?\u0005(.{8})");
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing server response (1)");
        }
        final String videoId = String.valueOf((long) ByteBuffer.wrap(matcher.group(1).getBytes("ISO-8859-1")).asDoubleBuffer().get());

        final List<Stream> list = new LinkedList<Stream>();
        matcher = getMatcherAgainstContent("(rtmpe?://[^\u0004]+)");
        while (matcher.find()) {
            list.add(new Stream(matcher.group(1), videoId, pubId));
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("Error parsing server response (3)");
        }
        return Collections.max(list).getRtmpSession();
    }

    private static class Stream implements Comparable<Stream> {
        private final String url;
        private final String playName;
        private final int bitrate;

        public Stream(final String s, final String videoId, final String pubId) throws ErrorDuringDownloadingException {
            Matcher matcher = PlugUtils.matcher("^(.+?)/&(.+)$", s);
            if (!matcher.find()) {
                logger.warning(s);
                throw new PluginImplementationException("Error parsing stream URL");
            }
            final String params = String.format("?videoId=%s&lineUpId=&pubId=%s&playerId=1707001745001&affiliateId=",
                    videoId, pubId);
            url = matcher.group(1) + params;
            playName = matcher.group(2) + params;
            matcher = PlugUtils.matcher("\\b(\\d+)k\\b", playName);
            if (!matcher.find()) {
                logger.warning(s);
                throw new PluginImplementationException("Stream bitrate not found");
            }
            bitrate = Integer.parseInt(matcher.group(1));
            logger.info("Found stream: " + this);
        }

        public RtmpSession getRtmpSession() throws ErrorDuringDownloadingException {
            logger.info("Downloading stream: " + this);
            return new RtmpSession(url, playName);
        }

        @Override
        public int compareTo(final Stream that) {
            return Integer.valueOf(this.bitrate).compareTo(that.bitrate);
        }

        @Override
        public String toString() {
            return "Stream{" +
                    "url='" + url + '\'' +
                    ", playName='" + playName + '\'' +
                    ", bitrate=" + bitrate +
                    '}';
        }
    }

}