package cz.vity.freerapid.plugins.services.crackle;

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
        Matcher matcher = getMatcherAgainstContent("(?s)<div id=\"doc-title\">(.+?)</div>");
        if (!matcher.find()) {
            matcher = getMatcherAgainstContent("(?s)<title>(.+?)</title>");
            if (!matcher.find()) {
                throw new PluginImplementationException("File name not found");
            }
        }
        String name = matcher.group(1).trim();
        matcher = PlugUtils.matcher("(?:Watch|Assista a(?:o filme)?|Ver(?: la película)?) (.+?) (?:Online Free|gratuito online|gratis en línea) - Crackle", name);
        if (matcher.find()) {
            name = matcher.group(1).trim();
            matcher = PlugUtils.matcher("(.+?), (.+?), (?:Season|Temporada) (\\d+), (?:Episode|Epis[óo]dio) (\\d+)", name);
            if (matcher.find()) {
                name = String.format("%s - S%02dE%02d - %s",
                        matcher.group(1).trim(),
                        Integer.parseInt(matcher.group(3)),
                        Integer.parseInt(matcher.group(4)),
                        matcher.group(2).trim());
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
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final String id = PlugUtils.getStringBetween(getContentAsString(), "StartPlayer (", ",");
            final String rtmpUrl = "rtmp://" + PlugUtils.getStringBetween(getContentAsString(), "strRtmpCdnUrl=\"", "\"");
            method = getMethodBuilder().setAction("/app/vidwall.ashx?flags=-1&fm=" + id + "&partner=20").toGetMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
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
        if (getContentAsString().contains("we couldn&rsquo;t find that page")
                || getContentAsString().contains("A página que você procura está indisponível ou não existe")
                || getContentAsString().contains("La página que buscas no se encuentra o no existe")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
        if (PlugUtils.find("<items title=\"[^\"]*?\" />", getContentAsString())) {
            throw new URLNotAvailableAnymoreException("Video is not available");
        }
    }

}