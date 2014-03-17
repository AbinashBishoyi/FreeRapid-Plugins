package cz.vity.freerapid.plugins.services.cbs;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class CbsFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(CbsFileRunner.class.getName());
    private final static String SWF_URL = "http://vidtech.cbsinteractive.com/player/2_9_2/CBSI_PLAYER.swf";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        //Server sometimes sends a 404 response even though everything is fine
        makeRedirectedRequest(getMethod);
        checkProblems();
        checkNameAndSize();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        try {
            PlugUtils.checkName(httpFile, getContentAsString(), "<span class=\"cbs-video-title H6_14\">", "</span>");
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

        HttpMethod method = getGetMethod(fileURL);
        makeRedirectedRequest(method);
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

        setFileStreamContentTypes(new String[0], new String[]{"application/smil"});
        method = getGetMethod("http://release.theplatform.com/content.select?format=SMIL&Tracking=true&balance=true&MBR=true&pid=" + pid);
        makeRedirectedRequest(method);
        checkProblems();

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
        rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
        rtmpSession.getConnectParams().put("pageUrl", fileURL);
        tryDownloadAndSaveFile(rtmpSession);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Not the page you were looking for")
                || getContentAsString().contains("Media is not available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("title=\"Unavailable\"")) {
            throw new NotRecoverableDownloadException("This video cannot be streamed in your geographical area");
        }
    }

}