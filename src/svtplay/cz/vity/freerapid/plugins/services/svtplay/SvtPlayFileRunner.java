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
        Matcher matcher = getMatcherAgainstContent("<title>\\s*(.+?)\\s*\\| SVT Play\\s*</title>");
        if (!matcher.find()) {
            throw new PluginImplementationException("Show name not found");
        }
        final String showName = PlugUtils.unescapeHtml(matcher.group(1));
        matcher = getMatcherAgainstContent("</span>\\s*<h2>(.+?)</h2>");
        if (!matcher.find()) {
            throw new PluginImplementationException("Episode name not found");
        }
        final String episodeName = PlugUtils.unescapeHtml(matcher.group(1));
        httpFile.setFileName(showName + " - " + episodeName + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
            final RtmpSession rtmpSession = getRtmpSession();
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("som du försöker nå saknas")
                || getContentAsString().contains("som du f&ouml;rs&ouml;ker n&aring; saknas")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private RtmpSession getRtmpSession() throws ErrorDuringDownloadingException {
        final String[] dynamicStreamStrings = PlugUtils.getStringBetween(getContentAsString(), "dynamicStreams=", "&amp;").split("\\|");
        if (dynamicStreamStrings.length == 0) {
            throw new PluginImplementationException("No streams found");
        }
        final List<Stream> dynamicStreams = new LinkedList<Stream>();
        for (final String s : dynamicStreamStrings) {
            logger.info(s);
            dynamicStreams.add(Stream.parse(s));
        }
        return Collections.min(dynamicStreams).getRtmpSession();
    }

    private static class Stream implements Comparable<Stream> {
        private final String url;
        private final int bitrate;

        public static Stream parse(final String properties) throws ErrorDuringDownloadingException {
            String url = null;
            int bitrate = -1;
            for (final String property : properties.split(",")) {
                if (property.startsWith("url:")) {
                    url = property.substring("url:".length());
                } else if (property.startsWith("bitrate:")) {
                    bitrate = Integer.parseInt(property.substring("bitrate:".length()));
                }
            }
            if (url == null || bitrate == -1) {
                logger.warning(properties);
                throw new PluginImplementationException("Error parsing stream parameters");
            }
            return new Stream(url, bitrate);
        }

        private Stream(final String url, final int bitrate) {
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
            return Integer.valueOf(that.bitrate).compareTo(this.bitrate);
        }
    }

}