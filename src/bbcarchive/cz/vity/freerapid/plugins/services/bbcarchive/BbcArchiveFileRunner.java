package cz.vity.freerapid.plugins.services.bbcarchive;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.tunlr.Tunlr;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class BbcArchiveFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(BbcArchiveFileRunner.class.getName());
    private final static String SWF_URL = "http://www.bbc.co.uk/emp/10player.swf";

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
        final String name = PlugUtils.getStringBetween(getContentAsString(), "<h1 class=\"main\">", "</h1>")
                .replaceAll("<[^<>]*>", "")
                .replace(" | ", " - ")
                .replace(": ", " - ");
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
            method = getMethodBuilder().setActionFromTextBetween("emp.setPlaylist(\"", "\")").toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            Matcher matcher = getMatcherAgainstContent("<connection kind=\"(.+?)\" identifier=\"(.+?)\" server=\"(.+?)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing playlist");
            }
            method = getMethodBuilder()
                    .setAction("http://www.bbc.co.uk/mediaselector/4/gtis/")
                    .setParameter("server", matcher.group(3))
                    .setParameter("identifier", matcher.group(2))
                    .setParameter("kind", matcher.group(1))
                    .setParameter("application", "ondemand")
                    .setParameter("cb", String.valueOf(new Random().nextInt(100000)))
                    .toGetMethod();
            if (!client.getSettings().isProxySet()) {
                Tunlr.setupMethod(method);
            }
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            matcher = getMatcherAgainstContent("<error[^<>]*>(.+?)</error>");
            if (matcher.find()) {
                final String id = matcher.group(1);
                if (id.equals("notavailable")) {
                    throw new URLNotAvailableAnymoreException("Playlist not found");
                } else if (id.equals("notukerror")) {
                    throw new NotRecoverableDownloadException("This video is not available in your area");
                } else {
                    throw new NotRecoverableDownloadException("Error fetching playlist: '" + id + "'");
                }
            }
            final String server = PlugUtils.getStringBetween(getContentAsString(), "<server>", "</server>");
            final String identifier = PlugUtils.getStringBetween(getContentAsString(), "<identifier>", "</identifier>");
            final String application = PlugUtils.getStringBetween(getContentAsString(), "<application>", "</application>")
                    + "?" + PlugUtils.replaceEntities(PlugUtils.getStringBetween(getContentAsString(), "<token>", "</token>"));
            final RtmpSession rtmpSession = new RtmpSession(server, 1935, application, identifier);
            rtmpSession.getConnectParams().put("pageUrl", fileURL);
            rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Page not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}