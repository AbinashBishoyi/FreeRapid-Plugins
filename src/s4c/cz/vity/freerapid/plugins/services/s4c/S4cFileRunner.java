package cz.vity.freerapid.plugins.services.s4c;

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
class S4cFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(S4cFileRunner.class.getName());

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
        String name = PlugUtils.getStringBetween(getContentAsString(), "<h2 id=\"programme-title\">", "</h2>");
        final Matcher matcher = getMatcherAgainstContent("<li class=\"current-episode\">\\s*<a\\b[^<>]*>\\s*(.+?)\\s*<");
        if (!matcher.find()) {
            throw new PluginImplementationException("Episode name not found");
        }
        final String episode = matcher.group(1);
        if (!name.endsWith(episode)) {
            name += " - " + episode;
        }
        httpFile.setFileName(name + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final String streamer = PlugUtils.getStringBetween(getContentAsString(), "streamer:\"", "\"");
            final String file = "mp4:" + PlugUtils.getStringBetween(getContentAsString(), "file:\"", "\"");
            final RtmpSession rtmpSession = new RtmpSession(streamer, file);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Programme expired")
                || getContentAsString().contains("Mae'r rhaglen hon wedi dod i ben")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}