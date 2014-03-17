package cz.vity.freerapid.plugins.services.rte;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.rtmp.SwfVerificationHelper;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class RteFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(RteFileRunner.class.getName());

    private final static String SWF_URL = "http://www.rte.ie/player/assets/player_403.swf";
    private final static SwfVerificationHelper helper = new SwfVerificationHelper(SWF_URL);

    private String feedURL;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod method = getGetMethod(getFeedURL());
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getFeedURL() throws ErrorDuringDownloadingException {
        if (feedURL == null) {
            final Matcher matcher = PlugUtils.matcher("v=(\\d+)", fileURL);
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing file URL");
            }
            feedURL = "http://dj.rte.ie/vodfeeds/feedgenerator/videos/show/?id=" + matcher.group(1);
        }
        return feedURL;
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<title type=\"text\">", "</title>");
        httpFile.setFileName(PlugUtils.unescapeHtml(httpFile.getFileName()) + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();

        final Matcher matcher = getMatcherAgainstContent("<media:content url=\"(rtmp.+?)\"");
        if (!matcher.find()) {
            throw new PluginImplementationException("Stream URL not found");
        }
        //we don't support RTMPT, but plain RTMP is lighter on the server so I don't think they mind
        final String stream = matcher.group(1).replaceFirst("rtmpt", "rtmp");

        final RtmpSession rtmpSession = new RtmpSession(stream);
        rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
        rtmpSession.getConnectParams().put("pageUrl", fileURL);
        helper.setSwfVerification(rtmpSession, client);

        tryDownloadAndSaveFile(rtmpSession);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("<title>RTE Error")) {
            throw new URLNotAvailableAnymoreException("The content you are trying to access is not currently available for viewing, as it has expired, been removed, or is restricted to another territory");
        }
    }

}