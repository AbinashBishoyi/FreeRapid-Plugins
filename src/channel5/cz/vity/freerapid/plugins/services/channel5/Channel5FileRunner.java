package cz.vity.freerapid.plugins.services.channel5;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

import java.nio.ByteBuffer;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class Channel5FileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(Channel5FileRunner.class.getName());

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
        final String name;
        final Matcher matcher = getMatcherAgainstContent("<h3 class=\"episode_header\"><span class=\"sifr_grey_light\">(?:(?:Season|Series) (\\d+) \\- Episode (\\d+))?(?:: )?(.+?)?</span></h3>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found (1)");
        }
        final Matcher matcher2 = getMatcherAgainstContent("<h[12]><span class=\"sifr_white\">(.+?)</span></h[12]>");
        if (!matcher2.find()) {
            throw new PluginImplementationException("File name not found (2)");
        }
        final String program = matcher2.group(1);
        final String seasonNum = matcher.group(1);
        final String episodeNum = matcher.group(2);
        final String episode = matcher.group(3);
        final boolean episodeSet = episode != null;
        final boolean seasonAndEpisodeNumSet = seasonNum != null && episodeNum != null;
        if (episodeSet && seasonAndEpisodeNumSet) {
            final int seasonNumI = Integer.parseInt(seasonNum);
            final int episodeNumI = Integer.parseInt(episodeNum);
            name = String.format("%s - S%02dE%02d - %s", program, seasonNumI, episodeNumI, episode);
        } else if (episodeSet && !seasonAndEpisodeNumSet) {
            name = String.format("%s - %s", program, episode);
        } else if (seasonAndEpisodeNumSet) {
            final int seasonNumI = Integer.parseInt(seasonNum);
            final int episodeNumI = Integer.parseInt(episodeNum);
            name = String.format("%s - S%02dE%02d", program, seasonNumI, episodeNumI);
        } else {
            name = program;
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
            setFileStreamContentTypes(new String[0], new String[]{"application/x-amf"});
            method = getPostMethod("http://c.brightcove.com/services/messagebroker/amf?playerId=1707001743001");
            ((PostMethod) method).setRequestEntity(new ByteArrayRequestEntity(getAmfPostData(), "application/x-amf"));
            // bypass the geocheck
            method.setRequestHeader("X-Forwarded-For", "212.169.3.171");
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            if (getContentAsString().contains("The video you are trying to watch cannot be viewed from your current country or location")) {
                throw new NotRecoverableDownloadException("The video you are trying to watch cannot be viewed from your current country or location");
            }
            logger.info(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("this page does not exist")) {
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

}