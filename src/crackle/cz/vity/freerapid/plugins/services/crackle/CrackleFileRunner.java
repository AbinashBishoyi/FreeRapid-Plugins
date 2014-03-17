package cz.vity.freerapid.plugins.services.crackle;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.rtmp.SwfVerificationHelper;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class CrackleFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(CrackleFileRunner.class.getName());
    private final static String SWF_URL = "http://www.crackle.com/flash/ReferrerRedirect.ashx";
    private final static SwfVerificationHelper helper = new SwfVerificationHelper(SWF_URL);

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
        Matcher matcher = getMatcherAgainstContent("<title>\\s*Watch (.+?) Online Free - Crackle\\s*</title>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        String name = matcher.group(1);
        matcher = PlugUtils.matcher("(.+?), (.+?), Season (\\d+), Episode (\\d+)", name);
        if (matcher.find()) {
            name = String.format("%s - S%02dE%02d - %s",
                    matcher.group(1),
                    Integer.parseInt(matcher.group(3)),
                    Integer.parseInt(matcher.group(4)),
                    matcher.group(2));
        }
        httpFile.setFileName(name + ".flv");
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
            final String id = PlugUtils.getStringBetween(getContentAsString(), "StartPlayer (", ",");
            final String rtmpUrl = "rtmp://" + PlugUtils.getStringBetween(getContentAsString(), "strRtmpCdnUrl=\"", "\"");
            // This cookie is all it takes to bypass their geo restrictions.
            addCookie(new Cookie(".crackle.com", "GR", "348", "/", 86400, false));
            method = getGetMethod("http://www.crackle.com/app/vidwall.ashx?flags=-1&fm=" + id + "&partner=20");
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            logger.info(getContentAsString());
            final Matcher matcher = getMatcherAgainstContent("<i\\b[^<>]+?\\bp=\"([^<>\"]+?)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Play name not found");
            }
            final String playName = "mp4:" + matcher.group(1) + "480p.mp4";
            final RtmpSession rtmpSession = new RtmpSession(rtmpUrl, playName);
            rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
            rtmpSession.getConnectParams().put("pageUrl", fileURL);
            helper.setSwfVerification(rtmpSession, client);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("we couldn&rsquo;t find that page")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
        if (getContentAsString().contains("<items title=\"Newest\" />")) {
            throw new NotRecoverableDownloadException("Crackle is unavailable in your region");
        }
    }

}