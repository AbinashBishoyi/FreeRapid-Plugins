package cz.vity.freerapid.plugins.services.bigflix;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.rtmp.SwfVerificationHelper;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class BigFlixFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(BigFlixFileRunner.class.getName());

    private final static String SWF_URL = "http://www.bigflix.com/apache_file/flash/Player.swf";
    private final static SwfVerificationHelper helper = new SwfVerificationHelper(SWF_URL);

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<h3>\\s*(?:[^<>]+?:|<img[^<>]*?>)\\s*([^<>]+?)\\s*(?:<span class=\"name_part\">([^<>]+?)</span>\\s*)?</h3>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        String name = matcher.group(1);
        String namePart = matcher.group(2);
        if (namePart != null) {
            name += " - " + namePart.replaceAll("\\s+", " ").replace('|', '-');
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
            final String url = new String(
                    Base64.decodeBase64(
                            URLDecoder.decode(
                                    PlugUtils.getStringBetween(getContentAsString(), "&file=", "&"), "UTF-8")), "UTF-8");
            logger.info("url = " + url);
            final Matcher matcher = PlugUtils.matcher("(rtmpe?)://([^/:]+)(:[0-9]+)?/([^/]+/[^/]+)/(.*)", url);
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing stream URL");
            }
            final RtmpSession rtmpSession = new RtmpSession(matcher.group(2), getPort(matcher.group(3)), matcher.group(4), getPlayName(matcher.group(5)), matcher.group(1));
            rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
            rtmpSession.getConnectParams().put("pageUrl", fileURL);
            helper.setSwfVerification(rtmpSession, client);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private int getPort(final String s) {
        return s == null ? 1935 : Integer.parseInt(s);
    }

    private String getPlayName(final String s) {
        if (s.contains(".mp4")) {
            return "mp4:" + s;
        } else {
            return s;
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("what-new.gif") || getContentAsString().contains("404 Not Found") || getContentAsString().contains("HTTP Status 404")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("download_only.gif")) {
            throw new NotRecoverableDownloadException("This video must be bought or rented");
        }
    }

}