package cz.vity.freerapid.plugins.services.rtlnow;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.rtmp.SwfVerificationHelper;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class RtlNowFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(RtlNowFileRunner.class.getName());

    private final static String SWF_URL = "http://rtl-now.rtl.de/includes/vodplayer.swf";
    private final static SwfVerificationHelper helper = new SwfVerificationHelper(SWF_URL);

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems(method);
            checkNameAndSize();
        } else {
            checkProblems(method);
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String name = PlugUtils.getStringBetween(getContentAsString(), "<title>", "</title>");
        httpFile.setFileName(name + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems(final HttpMethod method) throws Exception {
        if ("http://rtl-now.rtl.de/".equals(method.getURI().toString())
                || method.getURI().toString().endsWith(".php")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems(method);
            checkNameAndSize();
            Matcher matcher = getMatcherAgainstContent("data\\s*:\\s*'(.+?)'");
            if (!matcher.find()) {
                throw new PluginImplementationException("XML URL not found");
            }
            String url = URLDecoder.decode(matcher.group(1), "UTF-8");
            method = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
            if (makeRedirectedRequest(method)) {
                matcher = getMatcherAgainstContent("<filename[^<>]*?>(.+?)</filename>");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Stream URL not found");
                }
                url = matcher.group(1);
                if (url.startsWith("<![CDATA[")) {
                    url = url.substring("<![CDATA[".length(), url.length() - "]]>".length());
                }
                final RtmpSession rtmpSession = new RtmpSession(url);
                rtmpSession.setPlayName(correctPlayName(rtmpSession.getPlayName()));
                rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
                rtmpSession.getConnectParams().put("pageUrl", fileURL);
                helper.setSwfVerification(rtmpSession, client);
                tryDownloadAndSaveFile(rtmpSession);
            } else {
                checkProblems(method);
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems(method);
            throw new ServiceConnectionProblemException();
        }
    }

    private String correctPlayName(String playName) {
        if (playName.endsWith(".flv")) {
            playName = playName.substring(0, playName.length() - ".flv".length());
        } else if (playName.endsWith(".mp4") || playName.endsWith(".f4v") || playName.endsWith(".mov")) {
            playName = "mp4:" + playName;
        }
        logger.info("playName = " + playName);
        return playName;
    }

}