package cz.vity.freerapid.plugins.services.rutube;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.rtmp.SwfVerificationHelper;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class RuTubeFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(RuTubeFileRunner.class.getName());

    private final static String SWF_URL = "http://rutube.ru/player.swf";
    private final static SwfVerificationHelper helper = new SwfVerificationHelper(SWF_URL);

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setPageEncoding("KOI8-R");
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
        final String name = PlugUtils.getStringBetween(getContentAsString(), "<meta property=\"og:title\" content=\"", "\" />");
        httpFile.setFileName(name + ".flv");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<span class=\"icn-size\" itemprop=\"contentSize\">", "</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        setPageEncoding("KOI8-R");
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final Matcher matcher = getMatcherAgainstContent("player\\.swf\\?.*?file=([^&='\"]+)");
            if (!matcher.find()) {
                throw new PluginImplementationException("Playlist URL not found");
            }
            String playlistUrl = URLDecoder.decode(matcher.group(1), "UTF-8");
            playlistUrl = playlistUrl.substring(0, playlistUrl.lastIndexOf('.') + 1) + "f4m";
            logger.info("playlistUrl = " + playlistUrl);
            setPageEncoding("UTF-8");
            method = getGetMethod(playlistUrl);
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final String baseUrl = PlugUtils.getStringBetween(getContentAsString(), "<baseURL>", "</baseURL>");
            final String mediaUrl = PlugUtils.replaceEntities(
                    PlugUtils.getStringBetween(getContentAsString(), "<media url=\"", "\""));
            logger.info("baseUrl = " + baseUrl);
            logger.info("mediaUrl = " + mediaUrl);
            final RtmpSession rtmpSession = createRtmpSession(baseUrl, mediaUrl);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Нет такой страницы")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private RtmpSession createRtmpSession(final String baseUrl, final String mediaUrl) throws Exception {
        final int index = mediaUrl.indexOf("mp4:");
        if (index == -1) {
            throw new PluginImplementationException("Error parsing media URL");
        }
        final String tcUrl = baseUrl + mediaUrl.substring(0, index);
        final String playName = mediaUrl.substring(index);
        logger.info("tcUrl = " + tcUrl);
        logger.info("playName = " + playName);
        final RtmpSession rtmpSession = new RtmpSession(tcUrl, playName);
        rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
        rtmpSession.getConnectParams().put("pageUrl", fileURL);
        helper.setSwfVerification(rtmpSession, client);
        return rtmpSession;
    }

}