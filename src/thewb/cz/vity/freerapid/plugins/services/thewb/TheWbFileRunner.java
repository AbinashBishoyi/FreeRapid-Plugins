package cz.vity.freerapid.plugins.services.thewb;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.tunlr.Tunlr;
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
class TheWbFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(TheWbFileRunner.class.getName());
    private final static String SWF_URL = "http://www.thewb.com/player/wbphasethree/wbvideoplayer.swf";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (!client.getSettings().isProxySet()) {
            Tunlr.setupMethod(method);
        }
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        String name = PlugUtils.getStringBetween(getContentAsString(), "<meta name=\"title\" content=\"", "\"");
        name = name.replace(": ", " - ");
        Matcher matcher = getMatcherAgainstContent("<span class=\"episode\">Season (\\d+)\\s*:\\s*Episode (\\d+)");
        if (matcher.find()) {
            final String[] split = name.split("\\-", 2);
            if (split.length >= 2) {
                name = String.format("%s - S%02dE%02d - %s",
                        split[0].trim(),
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        split[1].trim());
            }
        }
        httpFile.setFileName(name + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (!client.getSettings().isProxySet()) {
            Tunlr.setupMethod(method);
        }
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final String mediaKey = PlugUtils.getStringBetween(getContentAsString(), "mediaKey=", "\"");
            setFileStreamContentTypes(new String[0], new String[]{"application/json"});
            method = getGetMethod("http://metaframe.digitalsmiths.tv/v2/WBtv/assets/" + mediaKey + "/partner/146?format=json");
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            logger.info(getContentAsString());
            final RtmpSession rtmpSession = getRtmpSession();
            rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
            rtmpSession.getConnectParams().put("pageUrl", fileURL);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("The WB can only be viewed in the United States")) {
            throw new NotRecoverableDownloadException("The WB can only be viewed in the United States");
        }
    }

    private RtmpSession getRtmpSession() throws ErrorDuringDownloadingException {
        final List<Stream> list = new LinkedList<Stream>();
        final Matcher matcher = getMatcherAgainstContent("\"bitrate\"\\s*:\\s*\"(\\d+)\",\\s*\"uri\"\\s*:\\s*\"(.+?)\"");
        while (matcher.find()) {
            list.add(new Stream(matcher.group(2), Integer.parseInt(matcher.group(1))));
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
            return new RtmpSession(url);
        }

        @Override
        public int compareTo(final Stream that) {
            return Integer.valueOf(this.bitrate).compareTo(that.bitrate);
        }
    }

}