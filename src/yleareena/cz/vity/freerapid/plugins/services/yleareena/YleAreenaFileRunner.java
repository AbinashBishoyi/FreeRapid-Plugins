package cz.vity.freerapid.plugins.services.yleareena;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
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
class YleAreenaFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(YleAreenaFileRunner.class.getName());

    private String id;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getGetMethod(checkFileUrl());
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        httpFile.setFileName(PlugUtils.unescapeHtml(PlugUtils.getStringBetween(getContentAsString(), "<h1 class=\"cliptitle\">", "</h1>"))
                .replace(": ", " - ") + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private String checkFileUrl() throws ErrorDuringDownloadingException {
        // "video" works for audio too
        return "http://areena.yle.fi/video/" + getId();
    }

    private String getId() throws ErrorDuringDownloadingException {
        if (id == null) {
            final Matcher matcher = PlugUtils.matcher("/(?:video/|audio/|player/index\\.php\\?clip=)(\\d+)", fileURL);
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing file URL");
            }
            id = matcher.group(1);
        }
        return id;
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod method = getGetMethod(checkFileUrl());
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final RtmpSession rtmpSession = new RtmpSession("flashk.yle.fi", 1935, "AreenaServer", null /* will be set by custom PacketHandler */);
            rtmpSession.addPacketHandler(new YleAreenaPacketHandler(getId()));
            rtmpSession.getConnectParams().put("swfUrl", "http://areena.yle.fi/player/Application.swf");
            rtmpSession.getConnectParams().put("pageUrl", fileURL);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("etsimääsi ohjelmaa tai ohjelmaklippiä ei löytynyt")
                || getContentAsString().contains("Osoitteemme ovat muuttuneet")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}