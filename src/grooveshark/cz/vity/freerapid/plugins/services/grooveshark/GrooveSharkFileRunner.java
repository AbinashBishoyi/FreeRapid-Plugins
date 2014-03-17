package cz.vity.freerapid.plugins.services.grooveshark;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.util.Formatter;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class GrooveSharkFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GrooveSharkFileRunner.class.getName());
    private final static String REQUEST_URL = "http://grooveshark.com/more.php?";
    private final static String uuid = UUID.randomUUID().toString().toUpperCase(Locale.ENGLISH);

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final String sessionId = getSessionId();
        final String communicationToken = getCommunicationToken(sessionId);
        final String songId = getSongIdFromSongToken(sessionId, communicationToken, getSongToken());
        final HttpMethod method = getStreamKeyFromSongId(sessionId, communicationToken, songId);
        if (!tryDownloadAndSaveFile(method)) {
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private String getSongToken() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher(".+/([^\\?]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        return matcher.group(1);
    }

    private String getSessionId() throws Exception {
        final HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        final Cookie phpsessid = getCookieByName("PHPSESSID");
        if (phpsessid == null) {
            throw new PluginImplementationException("Session ID not found");
        }
        return phpsessid.getValue();
    }

    private String getCommunicationToken(final String sessionId) throws Exception {
        final String content = String.format("{\"parameters\":{\"secretKey\":\"%s\"},\"header\":{\"clientRevision\":\"20110606\",\"uuid\":\"%s\",\"country\":{\"IPR\":\"10741\",\"ID\":\"67\",\"CC1\":\"0\",\"CC4\":\"0\",\"CC3\":\"0\",\"CC2\":\"4\"},\"client\":\"htmlshark\",\"privacy\":0,\"session\":\"%s\"},\"method\":\"getCommunicationToken\"}",
                DigestUtils.md5Hex(sessionId), uuid, sessionId);
        final PostMethod method = getPostMethod(REQUEST_URL + "getCommunicationToken");
        method.setRequestEntity(new StringRequestEntity(content, null, null));
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        final Matcher matcher = getMatcherAgainstContent("\"result\"\\s*:\\s*\"(.+?)\"");
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing response (1)");
        }
        return matcher.group(1);
    }

    private String getSongIdFromSongToken(final String sessionId, final String communicationToken, final String songToken) throws Exception {
        final String content = String.format("{\"header\":{\"client\":\"htmlshark\",\"clientRevision\":\"20110606\",\"privacy\":0,\"country\":{\"ID\":\"67\",\"CC1\":\"0\",\"CC2\":\"4\",\"CC3\":\"0\",\"CC4\":\"0\",\"IPR\":\"10741\"},\"uuid\":\"%s\",\"session\":\"%s\",\"token\":\"%s\"},\"method\":\"getSongFromToken\",\"parameters\":{\"token\":\"%s\",\"country\":{\"ID\":\"67\",\"CC1\":\"0\",\"CC2\":\"4\",\"CC3\":\"0\",\"CC4\":\"0\",\"IPR\":\"10741\"}}}",
                uuid, sessionId, getRequestToken("getSongFromToken", "backToTheScienceLab", communicationToken), songToken);
        final PostMethod method = getPostMethod(REQUEST_URL + "getSongFromToken");
        method.setRequestEntity(new StringRequestEntity(content, null, null));
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        if (getContentAsString().contains("\"result\":[]")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        checkName();
        final Matcher matcher1 = getMatcherAgainstContent("\"SongID\"\\s*:\\s*\"(.+?)\"");
        if (!matcher1.find()) {
            throw new PluginImplementationException("Error parsing response (2)");
        }
        return matcher1.group(1);
    }

    private HttpMethod getStreamKeyFromSongId(final String sessionId, final String communicationToken, final String songId) throws Exception {
        final String content = String.format("{\"parameters\":{\"songID\":%s,\"mobile\":false,\"country\":{\"ID\":\"67\",\"CC2\":\"4\",\"CC4\":\"0\",\"CC1\":\"0\",\"IPR\":\"10741\",\"CC3\":\"0\"},\"prefetch\":false},\"header\":{\"token\":\"%s\",\"clientRevision\":\"20110606\",\"uuid\":\"%s\",\"country\":{\"ID\":\"67\",\"CC2\":\"4\",\"CC4\":\"0\",\"CC1\":\"0\",\"IPR\":\"10741\",\"CC3\":\"0\"},\"client\":\"jsqueue\",\"privacy\":0,\"session\":\"%s\"},\"method\":\"getStreamKeyFromSongIDEx\"}",
                songId, getRequestToken("getStreamKeyFromSongIDEx", "bewareOfBearsharktopus", communicationToken), uuid, sessionId);
        final PostMethod method = getPostMethod(REQUEST_URL + "getStreamKeyFromSongIDEx");
        method.setRequestEntity(new StringRequestEntity(content, null, null));
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        final Matcher matcher1 = getMatcherAgainstContent("\"streamKey\"\\s*:\\s*\"(.+?)\"");
        if (!matcher1.find()) {
            throw new PluginImplementationException("Error parsing response (3)");
        }
        final Matcher matcher2 = getMatcherAgainstContent("\"ip\"\\s*:\\s*\"(.+?)\"");
        if (!matcher2.find()) {
            throw new PluginImplementationException("Error parsing response (4)");
        }
        return getMethodBuilder()
                .setAction("http://" + matcher2.group(1) + "/stream.php")
                .setParameter("streamKey", matcher1.group(1))
                .setReferer(null)
                .toPostMethod();
    }

    private String getRequestToken(final String request, final String salt, final String communicationToken) {
        final String random = new Formatter().format("%1$06x", new Random().nextInt(0xFFFFFF)).toString();
        return random + DigestUtils.shaHex(request + ":" + communicationToken + ":" + salt + ":" + random);
    }

    private void checkName() throws ErrorDuringDownloadingException {
        final Matcher matcher1 = getMatcherAgainstContent("\"Name\"\\s*:\\s*\"(.+?)\"");
        if (!matcher1.find()) {
            throw new PluginImplementationException("Song name not found");
        }
        final Matcher matcher2 = getMatcherAgainstContent("\"ArtistName\"\\s*:\\s*\"(.+?)\"");
        if (!matcher2.find()) {
            throw new PluginImplementationException("Artist name not found");
        }
        httpFile.setFileName(matcher2.group(1) + " - " + matcher1.group(1) + ".mp3");
    }

}