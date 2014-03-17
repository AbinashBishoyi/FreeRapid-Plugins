package cz.vity.freerapid.plugins.services.mog;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.tunlr.Tunlr;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u2
 */
class MogFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(MogFileRunner.class.getName());

    //Introduce apiToken as static, because apparently they track different apiToken as different device.
    //If different apiToken detected, they send "Another device is currently streaming error message".
    //So we have to save the apiToken received after login, to be used for the rest of session.
    private static String apiToken = null; //safe, assuming maxdownload=1

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        if (isAlbum()) { //album
            PlugUtils.checkName(httpFile, content, "\"album_name\":\"", "\"");
        } else { //track
            final String album = PlugUtils.getStringBetween(content, "\"album_name\":\"", "\"").trim();
            final String track = PlugUtils.getStringBetween(content, "\"track_name\":\"", "\"").trim();
            httpFile.setFileName(String.format("%s - %s.flv", album, track));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        if (apiToken == null) {
            login();
            apiToken = PlugUtils.getStringBetween(getContentAsString(), "\"api_token\":\"", "\"");
        }
        final String mediaId = getMediaIdFromUrl();
        HttpMethod method = getMediaInfoMethod(mediaId);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        checkNameAndSize(getContentAsString());

        if (isAlbum()) {
            parseAlbum();
        } else {
            //to make sure only one track played at one time, send stop playback message
            addCookie(new Cookie(".mog.com", "mogger_alert", "alerted", "/", 86400, false));
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("https://mog.com/v2/playback/stop.json")
                    .setParameter("track_id", mediaId)
                    .setParameter("ts", String.valueOf(System.currentTimeMillis() / 1000))
                    .setParameter("api_token", apiToken)
                    .setParameter("allow_nonstreamable_token", "1")
                    .setAjax()
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                logger.warning("Failed sending stop playback message");
            }
            logger.info("Stop playback response : " + getContentAsString());
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(String.format("https://mog.com/v2/tracks/%s/stream.json", mediaId))
                    .setParameter("high_bw", "1")
                    .setParameter("scrobble", "0")
                    .setParameter("api_key", "chrome")
                    .setParameter("api_token", apiToken)
                    .setParameter("context", "track")
                    .setParameter("ts", String.valueOf(System.currentTimeMillis() / 1000))
                    .setParameter("allow_nonstreamable_token", "1")
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final String url = PlugUtils.getStringBetween(getContentAsString(), "\"location\":\"", "\"").replaceFirst(":80/", ":1935/"); //replace port 80 to 1935
            final String play = PlugUtils.getStringBetween(getContentAsString(), "\"resource\":\"", "\"");
            final RtmpSession rtmpSession = new RtmpSession(url, play);
            tryDownloadAndSaveFile(rtmpSession);
        }

    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Invalid reference or resource")
                || contentAsString.contains("we can't find that right now")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("only services The United States")) {
            throw new PluginImplementationException("MOG only services The United States, Puerto Rico, and The Virgin Islands");
        }
        if (contentAsString.contains("Another device is currently streaming")
                || contentAsString.contains("Another device registered to this user currently has streaming rights")) {
            throw new YouHaveToWaitException("Another device is currently streaming", 2 * 60);
        }
    }

    private String getMediaIdFromUrl() {
        return fileURL.substring(fileURL.lastIndexOf("/") + 1);
    }

    private boolean isAlbum() {
        return fileURL.matches("https?://(www\\.)?mog\\.com/(m#)?album/.+");
    }

    private HttpMethod getMediaInfoMethod(String mediaId) throws BuildMethodException {
        if (isAlbum()) { //album
            return getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(String.format("https://search.mog.com/v2/albums/%s.json", mediaId))
                    .toGetMethod();
        } else { //track
            return getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(String.format("https://search.mog.com/v2/tracks/%s.json", mediaId))
                    .toGetMethod();
        }
    }

    private void parseAlbum() throws Exception {
        final List<URI> uriList = new LinkedList<URI>();
        final Matcher trackMatcher = getMatcherAgainstContent("\"track_id\":\"(\\d+)\"");
        while (trackMatcher.find()) {
            final String url = String.format("https://mog.com/m#track/%s", trackMatcher.group(1));
            try {
                uriList.add(new URI(url));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        if (uriList.isEmpty()) {
            throw new PluginImplementationException("No song url found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
    }

    private void login() throws Exception {
        synchronized (MogFileRunner.class) {
            MogServiceImpl service = (MogServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No Mog account login information!");
                }
            }
            HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://mog.com/v2/auth/tokens.json")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("api_key", "chrome")
                    .setParameter("client_version", "1.0")
                    .setParameter("allow_nonstreamable_token", "1")
                    .setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .setAjax()
                    .toPostMethod();
            if (!client.getSettings().isProxySet()) {
                Tunlr.setupMethod(method);
            }
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            logger.info(getContentAsString());
            if (getContentAsString().contains("The email and password combination doesn't exist")
                    || getContentAsString().contains("Authorization Error")
                    || getContentAsString().contains("Either login or password is invalid")) {
                throw new BadLoginException("Invalid Mog account login information!");
            }
        }
    }

}