package cz.vity.freerapid.plugins.services.tv6play;

import cz.vity.freerapid.plugins.exceptions.*;
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
class Tv6PlayFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(Tv6PlayFileRunner.class.getName());

    private final static String SWF_URL = "http://flvplayer.viastream.viasat.tv/play/swf/player110516.swf";
    private final static SwfVerificationHelper helper = new SwfVerificationHelper(SWF_URL);

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
        final Matcher matcher = getMatcherAgainstContent("<title>(.+?) \\| TV[368] Play</title>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(matcher.group(1) + ".flv");
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
            method = getGetMethod("http://viastream.viasat.tv/PlayProduct/" + getId());
            if (makeRedirectedRequest(method)) {
                checkProblems();
                Matcher matcher = getMatcherAgainstContent("(?s)<Video>(.+?)</Video>");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Error parsing stream info file (1)");
                }
                matcher = PlugUtils.matcher("<Url><!\\[CDATA\\[(.+?)\\]\\]></Url>", matcher.group(1));
                if (!matcher.find()) {
                    throw new PluginImplementationException("Error parsing stream info file (2)");
                }
                String url = matcher.group(1);
                if (url.startsWith("http")) {
                    method = getGetMethod(url);
                    if (!makeRedirectedRequest(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();
                    url = PlugUtils.getStringBetween(getContentAsString(), "<Url>", "</Url>");
                }
                final RtmpSession rtmpSession = new RtmpSession(url);
                rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
                rtmpSession.getConnectParams().put("pageUrl", fileURL);
                helper.setSwfVerification(rtmpSession, client);
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

    private String getId() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("/play/(\\d+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        return matcher.group(1);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("URL:en hittades inte")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("due to rights limitations this content can not be shown in your country")) {
            throw new NotRecoverableDownloadException("Due to rights limitations this content can not be shown in your country");
        }
    }

}