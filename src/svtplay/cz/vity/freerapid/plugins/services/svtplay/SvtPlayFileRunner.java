package cz.vity.freerapid.plugins.services.svtplay;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class SvtPlayFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(SvtPlayFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkName() throws ErrorDuringDownloadingException {
        final String name = PlugUtils.getStringBetween(getContentAsString(), "<title>", "| SVT Play</title>");
        httpFile.setFileName(PlugUtils.unescapeHtml(name) + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
            setFileStreamContentTypes(new String[0], new String[]{"application/json"});
            method = getGetMethod("http://www.svtplay.se/video/" + getIdFromUrl() + "?output=json");
            if (makeRedirectedRequest(method)) {
                final RtmpSession rtmpSession = getRtmpSession();
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

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("sidan finns inte")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String getIdFromUrl() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("/video/(\\d+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        return matcher.group(1);
    }

    private RtmpSession getRtmpSession() throws ErrorDuringDownloadingException {
        final List<Stream> list = new LinkedList<Stream>();
        final Matcher matcher = getMatcherAgainstContent("\"url\":\"([^\"]+)\",\"bitrate\":(\\d+),\"playerType\":\"flash\"");
        while (matcher.find()) {
            list.add(new Stream(matcher.group(1), Integer.parseInt(matcher.group(2))));
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No streams found");
        }
        return Collections.max(list).getRtmpSession();
    }

    private static class Stream implements Comparable<Stream> {
        private final String url;
        private final int bitrate;

        public Stream(final String url, final int bitrate) {
            this.url = url;
            this.bitrate = bitrate;
        }

        public RtmpSession getRtmpSession() throws ErrorDuringDownloadingException {
            final Matcher matcher = PlugUtils.matcher("(rtmp(?:e|t|s|te|ts)?)://([^/:]+)(:[0-9]+)?/([^/]+/[^/]+)/(.*)", url);
            if (!matcher.find()) {
                logger.warning(url);
                throw new PluginImplementationException("Invalid RTMP URL");
            }
            final String port = matcher.group(3);
            final String playName = matcher.group(5);
            return new RtmpSession(
                    matcher.group(2),
                    port == null ? 1935 : Integer.parseInt(port.substring(1)),
                    matcher.group(4),
                    playName.endsWith(".mp4") ? "mp4:" + playName : playName,
                    matcher.group(1));
        }

        @Override
        public int compareTo(final Stream that) {
            return Integer.valueOf(this.bitrate).compareTo(that.bitrate);
        }
    }

}