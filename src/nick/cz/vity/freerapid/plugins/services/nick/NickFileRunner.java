package cz.vity.freerapid.plugins.services.nick;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
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
class NickFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(NickFileRunner.class.getName());

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
        final Matcher matcher = getMatcherAgainstContent("<span content=\"([^<>]+?)\" property=\"media:title\"");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(matcher.group(1).replace(": ", " - ") + ".flv");
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

            Matcher matcher = getMatcherAgainstContent("type\\s*:\\s*\"(.+?)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("type not found");
            }
            final String type = matcher.group(1);
            matcher = getMatcherAgainstContent("site\\s*:\\s*\"(.+?)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("site not found");
            }
            final String site = matcher.group(1);
            matcher = getMatcherAgainstContent("cmsId\\s*:\\s*(\\d+)");
            if (!matcher.find()) {
                throw new PluginImplementationException("cmsId not found");
            }
            final String cmsId = matcher.group(1);

            final String mgid = "mgid:cms:" + type + ":" + site + ":" + cmsId;
            method = getGetMethod("http://www.nick.com/dynamo/video/data/mediaGen.jhtml?mgid=" + mgid);
            if (makeRedirectedRequest(method)) {
                final String url = getStreamUrl();
                if (!url.startsWith("rtmp")) {
                    throw new NotRecoverableDownloadException("This video is unavailable from your location");
                }
                final RtmpSession rtmpSession = new RtmpSession(url);
                if (!rtmpSession.getPlayName().startsWith("mp4:")) {
                    rtmpSession.setPlayName("mp4:" + rtmpSession.getPlayName());
                }
                rtmpSession.getConnectParams().put("pageUrl", fileURL);
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

    private String getStreamUrl() throws ErrorDuringDownloadingException {
        final List<Stream> list = new LinkedList<Stream>();
        final Matcher matcher = getMatcherAgainstContent("<rendition[^<>]+?bitrate=\"(\\d+)\"[^<>]+?>\\s*<src>([^<>]+?)</src>\\s*</rendition>");
        while (matcher.find()) {
            list.add(new Stream(matcher.group(2), Integer.parseInt(matcher.group(1))));
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No streams found");
        }
        return Collections.max(list).getUrl();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().isEmpty() || getContentAsString().contains("The page you're looking for")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private static class Stream implements Comparable<Stream> {
        private final String url;
        private final int bitrate;

        public Stream(final String url, final int bitrate) {
            this.url = url;
            this.bitrate = bitrate;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public int compareTo(final Stream that) {
            return Integer.valueOf(this.bitrate).compareTo(that.bitrate);
        }
    }

}