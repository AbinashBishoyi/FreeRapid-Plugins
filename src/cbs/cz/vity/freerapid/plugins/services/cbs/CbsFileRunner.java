package cz.vity.freerapid.plugins.services.cbs;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.interfaces.FileStreamRecognizer;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class CbsFileRunner extends AbstractRtmpRunner implements FileStreamRecognizer {
    private final static Logger logger = Logger.getLogger(CbsFileRunner.class.getName());

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
        try {
            PlugUtils.checkName(httpFile, getContentAsString(), "<div id=\"info-container-left\">\n<p>", "</p>");
            httpFile.setFileName(httpFile.getFileName() + ".flv");
        } catch (PluginImplementationException e) {
            //ignore... not available in some videos
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        setClientParameter(DownloadClientConsts.FILE_STREAM_RECOGNIZER, this);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            String pid;
            try {
                pid = PlugUtils.getStringBetween(getContentAsString(), "var pid = \"", "\"");
            } catch (PluginImplementationException e) {
                final Matcher matcher = PlugUtils.matcher("pid=(.+?)(?:&.+?)?$", fileURL);
                if (!matcher.find()) {
                    throw new PluginImplementationException("PID not found");
                }
                pid = matcher.group(1);
            }
            method = getGetMethod("http://release.theplatform.com/content.select?format=SMIL&Tracking=true&balance=true&MBR=true&pid=" + pid);
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }

            if (getContentAsString().contains("Media is not available")) {
                throw new URLNotAvailableAnymoreException("File not found");
            }
            if (getContentAsString().contains("title=\"Unavailable\"")) {
                throw new NotRecoverableDownloadException("This video cannot be streamed in your geographical area");
            }

            final Matcher matcher = PlugUtils.matcher("<ref src=\"rtmp://(.+?)/(.+?)/(\\?.+?)<break>(.+?)\" title=\"(.+?)\"", PlugUtils.unescapeHtml(getContentAsString()));
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing SMIL response");
            }
            httpFile.setFileName(matcher.group(5) + ".flv");

            String playName = matcher.group(4);
            if (playName.contains(".mp4")) {
                playName = "mp4:" + playName;
            } else {
                playName = playName.replace(".flv", "");
            }

            final RtmpSession rtmpSession = new RtmpSession(matcher.group(1), 1935, matcher.group(2) + matcher.group(3), playName);
            rtmpSession.getConnectParams().put("swfUrl", "http://www.cbs.com/[[IMPORT]]/vidtech.cbsinteractive.com/player/2_4_1/CBSI_PLAYER.swf");
            rtmpSession.getConnectParams().put("pageUrl", fileURL);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Not the page you were looking for")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
    }

    @Override
    public boolean isStream(HttpMethod method, boolean showWarnings) {
        final Header h = method.getResponseHeader("Content-Type");
        if (h == null) return false;
        final String contentType = h.getValue().toLowerCase(Locale.ENGLISH);
        return (!contentType.startsWith("text/") && !contentType.contains("xml") && !contentType.startsWith("application/smil"));
    }

}