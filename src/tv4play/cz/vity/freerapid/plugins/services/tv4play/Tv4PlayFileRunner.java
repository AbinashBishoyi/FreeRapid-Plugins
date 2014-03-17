package cz.vity.freerapid.plugins.services.tv4play;

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
import org.apache.commons.httpclient.HttpStatus;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class Tv4PlayFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(Tv4PlayFileRunner.class.getName());

    private final static String SWF_URL = "http://www.tv4play.se/flash/tv4playflashlets.swf";
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
        final Matcher matcher = getMatcherAgainstContent("<title>(.+?) \\- TV4 Play</title>");
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
            method = getGetMethod("http://prima.tv4play.se/api/web/asset/" + getId() + "/play");
            // They send "Content-Encoding: utf-8", which is a violation of the HTTP spec.
            // As such, makeRedirectedRequest cannot be used here.
            if (client.getHTTPClient().executeMethod(method) != HttpStatus.SC_OK) {
                checkProblems();
                throw new URLNotAvailableAnymoreException("File not found");
            }
            final String content = method.getResponseBodyAsString();
            Matcher matcher = PlugUtils.matcher("<base>(.+?)</base>", content);
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing stream info file (1)");
            }
            final String baseUrl = matcher.group(1);
            matcher = PlugUtils.matcher("<url>(.+?)</url>", content);
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing stream info file (2)");
            }
            final String playName = matcher.group(1);
            final RtmpSession rtmpSession = new RtmpSession(baseUrl, playName);
            rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
            rtmpSession.getConnectParams().put("pageUrl", fileURL);
            helper.setSwfVerification(rtmpSession, client);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getId() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("videoid=(\\d+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        return matcher.group(1);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Länken du klickat på eller adressen du skrivit in leder ingenstans")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}