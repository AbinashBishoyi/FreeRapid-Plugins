package cz.vity.freerapid.plugins.services.dr;

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
class DrFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(DrFileRunner.class.getName());
    private final static String SWF_URL = "http://www.dr.dk/assets/swf/program-player.swf";

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
        final String name = PlugUtils.getStringBetween(getContentAsString(), "class=\"heading-xxxlarge\">", "</");
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
            checkNameAndSize();
            method = getMethodBuilder().setActionFromTextBetween("resource: \"", "\"").toGetMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final String streamUrl = getStreamUrl();
            if (streamUrl.startsWith("rtmp")) {
                final RtmpSession rtmpSession = new RtmpSession(streamUrl);
                rtmpSession.getConnectParams().put("pageUrl", fileURL);
                rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
                tryDownloadAndSaveFile(rtmpSession);
            } else {
                method = getGetMethod(streamUrl);
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("resource: \"\"")
                || getContentAsString().contains("Der er opstået en fejl")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String getStreamUrl() throws Exception {
        final List<Video> list = new LinkedList<Video>();
        final Matcher matcher = getMatcherAgainstContent("\"uri\":\"([^\"]+?)\"[^\\{\\}]*?\"bitrateKbps\":(\\d+)");
        while (matcher.find()) {
            list.add(new Video(matcher.group(1).replace("\\", ""), Integer.parseInt(matcher.group(2))));
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("Stream not found");
        }
        return Collections.max(list).getUrl();
    }

    private static class Video implements Comparable<Video> {
        private final String url;
        private final int quality;

        public Video(final String url, final int quality) {
            this.url = url;
            this.quality = quality;
            logger.info("Found stream: " + this);
        }

        public String getUrl() {
            logger.info("Downloading stream: " + this);
            return url;
        }

        @Override
        public int compareTo(final Video that) {
            return Integer.valueOf(this.quality).compareTo(that.quality);
        }

        @Override
        public String toString() {
            return "Video{" +
                    "url='" + url + '\'' +
                    ", quality=" + quality +
                    '}';
        }
    }

}