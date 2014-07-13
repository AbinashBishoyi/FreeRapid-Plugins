package cz.vity.freerapid.plugins.services.cbs;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.tunlr.Tunlr;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 * @author tong2shot
 */
class CbsFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(CbsFileRunner.class.getName());
    private final static String SWF_URL = "http://www.cbs.com/thunder/canplayer/canplayer.swf";
    private SettingsConfig config;

    private void setConfig() throws Exception {
        final CbsServiceImpl service = (CbsServiceImpl) getPluginService();
        config = service.getConfig();
    }

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
        final Matcher matcher = getMatcherAgainstContent("<title>(.+?) Video \\- (.+?) \\- CBS\\.com</title>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(matcher.group(1) + " - " + matcher.group(2) + ".flv");
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
            setConfig();
            String pid;
            try {
                pid = PlugUtils.getStringBetween(getContentAsString(), "pid = '", "'");
            } catch (final PluginImplementationException e) {
                final Matcher matcher = PlugUtils.matcher("pid=(.+?)(?:&.+?)?$", fileURL);
                if (!matcher.find()) {
                    throw new PluginImplementationException("PID not found");
                }
                pid = matcher.group(1);
            }
            setFileStreamContentTypes(new String[0], new String[]{"application/smil"});
            method = getGetMethod("http://link.theplatform.com/s/dJ5BDC/" + pid + "?format=SMIL&Tracking=true&mbr=true");
            if (!client.getSettings().isProxySet()) {
                Tunlr.setupMethod(method);
            }
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            String releaseContent = getContentAsString();

            String subtitleUrl = null;
            try {
                subtitleUrl = PlugUtils.getStringBetween(getContentAsString(), "<param name=\"ClosedCaptionURL\" value=\"", "\"");
            } catch (PluginImplementationException e) {
                LogUtils.processException(logger, e);
            }
            if (config.isDownloadSubtitles() && subtitleUrl != null && !subtitleUrl.isEmpty()) {
                SubtitleDownloader subtitleDownloader = new SubtitleDownloader();
                subtitleDownloader.downloadSubtitle(client, httpFile, subtitleUrl);
            }

            final RtmpSession rtmpSession = getRtmpSession(releaseContent);
            rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
            rtmpSession.getConnectParams().put("pageUrl", fileURL);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Not the page you were looking for")
                || getContentAsString().contains("Media is not available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("title=\"Unavailable\"")
                || getContentAsString().contains("This content is not available in your location")) {
            throw new NotRecoverableDownloadException("This content is not available in your location");
        }
    }

    private RtmpSession getRtmpSession(String releaseContent) throws ErrorDuringDownloadingException {
        final String baseUrl = PlugUtils.replaceEntities(PlugUtils.getStringBetween(releaseContent, "<meta base=\"", "\""));
        final Set<Stream> streamSet = new HashSet<Stream>();
        final Matcher matcher = PlugUtils.matcher("<video src=\"(.+?)\" system\\-bitrate=\"(\\d+)\" height=\"(\\d+)\"", releaseContent);
        while (matcher.find()) {
            Stream stream = new Stream(PlugUtils.unescapeHtml(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
            streamSet.add(stream);
        }
        if (streamSet.isEmpty()) {
            throw new PluginImplementationException("No streams found");
        }
        for (Stream stream : streamSet) {
            logger.info("Found stream: " + stream);
        }

        //select quality
        Stream selectedStream = null;
        final int LOWER_QUALITY_PENALTY = 10;
        int weight = Integer.MAX_VALUE;
        for (Stream stream : streamSet) {
            int deltaQ = stream.quality - config.getVideoQuality().getQuality();
            int tempWeight = (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ);
            if (tempWeight < weight) {
                weight = tempWeight;
                selectedStream = stream;
            }
        }
        if (selectedStream == null) {
            throw new PluginImplementationException("Unable to select stream");
        }
        int selectedQuality = selectedStream.quality;

        //select the highest bitrate for the selected quality
        int selectedBitrate = Integer.MIN_VALUE;
        for (Stream stream : streamSet) {
            if ((stream.quality == selectedQuality) && (stream.bitrate > selectedBitrate)) {
                selectedBitrate = stream.bitrate;
                selectedStream = stream;
            }
        }

        logger.info("Settings config: " + config);
        logger.info("Selected stream: " + selectedStream);
        return new RtmpSession(baseUrl, selectedStream.getPlayName());
    }

    private static class Stream implements Comparable<Stream> {
        private final String playName;
        private final int bitrate;
        private final int quality; //height as quality

        public Stream(final String playName, final int bitrate, final int quality) {
            this.playName = playName;
            this.bitrate = bitrate;
            this.quality = quality;
        }

        public String getPlayName() {
            if (playName.contains(".mp4") && !playName.startsWith("mp4:")) {
                return "mp4:" + playName;
            } else {
                return playName;
            }
        }

        @Override
        public int compareTo(final Stream that) {
            return Integer.valueOf(this.quality).compareTo(that.quality);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Stream stream = (Stream) o;
            return playName.equals(stream.playName);
        }

        @Override
        public int hashCode() {
            return playName.hashCode();
        }

        @Override
        public String toString() {
            return "Stream{" +
                    "playName='" + playName + '\'' +
                    ", bitrate=" + bitrate +
                    ", quality=" + quality +
                    '}';
        }
    }

}