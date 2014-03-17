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
    private final static String CLIENT_REVISION = "20130520";//sniff from requests
    private final static String SALT_1 = "frenchFriedDogs";//search for 'var r="' in app*.js
    private final static String SALT_2 = "nuggetsOfBaller";//search for 'secretKey' in decompressed .swf
    private String sessionId;
    private String uuid;

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final String songToken = getSongToken();
        getPreload();
        final String communicationToken = getCommunicationToken();
        final String songId = getSongIdFromSongToken(communicationToken, songToken);
        final HttpMethod method = getStreamKeyFromSongId(communicationToken, songId);
        if (!tryDownloadAndSaveFile(method)) {
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private String getSongToken() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("/s/[^/]+/(\\w+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        return matcher.group(1);
    }

    private void getPreload() throws Exception {
        final HttpMethod method = getGetMethod("http://grooveshark.com/preload.php");
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        final Cookie cookie = getCookieByName("PHPSESSID");
        if (cookie == null) {
            throw new PluginImplementationException("Session cookie not found");
        }
        sessionId = cookie.getValue();
        uuid = UUID.randomUUID().toString().toUpperCase(Locale.ROOT);
    }

    private String getCommunicationToken() throws Exception {
        final String content = String.format("{\"parameters\":{\"secretKey\":\"%s\"},\"header\":{\"clientRevision\":\"%s\",\"uuid\":\"%s\",\"country\":{\"IPR\":\"10741\",\"ID\":\"67\",\"CC1\":\"0\",\"CC4\":\"0\",\"CC3\":\"0\",\"CC2\":\"4\"},\"client\":\"htmlshark\",\"privacy\":0,\"session\":\"%s\"},\"method\":\"getCommunicationToken\"}",
                DigestUtils.md5Hex(sessionId), CLIENT_REVISION, uuid, sessionId);
        makePostRequest("https://grooveshark.com/more.php?getCommunicationToken", content);
        final Matcher matcher = getMatcherAgainstContent("\"result\"\\s*:\\s*\"(.+?)\"");
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing response (1)");
        }
        return matcher.group(1);
    }

    private String getSongIdFromSongToken(final String communicationToken, final String songToken) throws Exception {
        final String content = String.format("{\"header\":{\"client\":\"htmlshark\",\"clientRevision\":\"%s\",\"privacy\":0,\"country\":{\"ID\":\"67\",\"CC1\":\"0\",\"CC2\":\"4\",\"CC3\":\"0\",\"CC4\":\"0\",\"IPR\":\"10741\"},\"uuid\":\"%s\",\"session\":\"%s\",\"token\":\"%s\"},\"method\":\"getSongFromToken\",\"parameters\":{\"token\":\"%s\",\"country\":{\"ID\":\"67\",\"CC1\":\"0\",\"CC2\":\"4\",\"CC3\":\"0\",\"CC4\":\"0\",\"IPR\":\"10741\"}}}",
                CLIENT_REVISION, uuid, sessionId, getRequestToken("getSongFromToken", SALT_1, communicationToken), songToken);
        makePostRequest("http://grooveshark.com/more.php?getSongFromToken", content);
        checkName();
        final Matcher matcher1 = getMatcherAgainstContent("\"SongID\"\\s*:\\s*\"(.+?)\"");
        if (!matcher1.find()) {
            throw new PluginImplementationException("Error parsing response (2)");
        }
        return matcher1.group(1);
    }

    private HttpMethod getStreamKeyFromSongId(final String communicationToken, final String songId) throws Exception {
        final String content = String.format("{\"parameters\":{\"songID\":%s,\"mobile\":false,\"country\":{\"ID\":\"67\",\"CC2\":\"4\",\"CC4\":\"0\",\"CC1\":\"0\",\"IPR\":\"10741\",\"CC3\":\"0\"},\"prefetch\":false},\"header\":{\"token\":\"%s\",\"clientRevision\":\"%s\",\"uuid\":\"%s\",\"country\":{\"ID\":\"67\",\"CC2\":\"4\",\"CC4\":\"0\",\"CC1\":\"0\",\"IPR\":\"10741\",\"CC3\":\"0\"},\"client\":\"jsqueue\",\"privacy\":0,\"session\":\"%s\"},\"method\":\"getStreamKeyFromSongIDEx\"}",
                songId, getRequestToken("getStreamKeyFromSongIDEx", SALT_2, communicationToken), CLIENT_REVISION, uuid, sessionId);
        makePostRequest("http://grooveshark.com/more.php?getStreamKeyFromSongIDEx", content);
        final Matcher matcher1 = getMatcherAgainstContent("\"streamKey\"\\s*:\\s*\"(.+?)\"");
        if (!matcher1.find()) {
            throw new PluginImplementationException("Error parsing response (3)");
        }
        final Matcher matcher2 = getMatcherAgainstContent("\"ip\"\\s*:\\s*\"(.+?)\"");
        if (!matcher2.find()) {
            throw new PluginImplementationException("Error parsing response (4)");
        }
        return getMethodBuilder()
                .setReferer(fileURL)
                .setAction("http://" + matcher2.group(1) + "/stream.php")
                .setParameter("streamKey", matcher1.group(1))
                .toPostMethod();
    }

    private void makePostRequest(final String url, final String content) throws Exception {
        final PostMethod method = (PostMethod) getMethodBuilder()
                .setReferer(fileURL)
                .setAction(url)
                .toPostMethod();
        method.setRequestEntity(new StringRequestEntity(content, null, null));
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
    }

    private String getRequestToken(final String request, final String salt, final String communicationToken) {
        final String random = String.format("%06x", new Random().nextInt(0xFFFFFF));
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

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("\"result\":[]")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("invalid token")) {
            throw new PluginImplementationException("Invalid token, plugin outdated");
        }
        if (getContentAsString().contains("invalid client")) {
            throw new PluginImplementationException("Invalid client, plugin outdated");
        }
    }

}