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
            final Matcher matcher = getMatcherAgainstContent("http://video.rutube.ru/(\\d+)");
            if (!matcher.find()) {
                throw new PluginImplementationException("Track id not found");
            }
            final String trackId = matcher.group(1);
            method = getMethodBuilder()
                    .setReferer(String.format("%s/?hash=%s&referer=%s", SWF_URL, trackId, fileURL))
                    .setAction(String.format("http://rutube.ru/trackinfo/%s.xml?referer=%s", trackId, fileURL))
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final String playlistUrl = PlugUtils.getStringBetween(getContentAsString(), "<default>", "</default>") + "?referer=" + fileURL;
            logger.info("playlistUrl = " + playlistUrl);
            method = getGetMethod(playlistUrl);
            if (!makeRedirectedRequest(method)) {
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
        if (getContentAsString().contains("Запрашиваемая вами страница не найдена")) {
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