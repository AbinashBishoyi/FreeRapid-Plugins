package cz.vity.freerapid.plugins.services.crunchyroll;

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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class CrunchyRollFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(CrunchyRollFileRunner.class.getName());
    private static SwfVerificationHelper helper = null;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkName() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<title>(?:Watch |Music \\- )?(.+?)(?: \\- Crunchyroll)?</title>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(matcher.group(1) + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
            final String loaderSwfUrl = PlugUtils.getStringBetween(getContentAsString(), ".embedSWF(\"", "\"").replace("\\/", "/");
            final String configUrl = URLDecoder.decode(PlugUtils.getStringBetween(getContentAsString(), "\"config_url\":\"", "\""), "UTF-8");
            method = getMethodBuilder().setReferer(null).setAction(configUrl).setParameter("current_page", fileURL).toPostMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final String host = PlugUtils.getStringBetween(getContentAsString(), "<host>", "</host>");
            final String file = PlugUtils.replaceEntities(PlugUtils.getStringBetween(getContentAsString(), "<file>", "</file>"));
            final String playerSwfUrl = PlugUtils.replaceEntities(PlugUtils.getStringBetween(getContentAsString(), "<default:chromelessPlayerUrl>", "</default:chromelessPlayerUrl>"));
            final String swfUrl;
            try {
                swfUrl = new URI(loaderSwfUrl).resolve(new URI(playerSwfUrl)).toString();
            } catch (final URISyntaxException e) {
                throw new PluginImplementationException("Invalid SWF URL", e);
            }
            final RtmpSession rtmpSession = new RtmpSession(host, file);
            setSwfVerification(rtmpSession, swfUrl);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("No media found") || getContentAsString().contains("we were unable to find the page you were looking for")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("This video is not available from this website")) {
            throw new URLNotAvailableAnymoreException("This video is not available from this website");
        }
        if (getContentAsString().contains("You are watching a sample clip")) {
            throw new URLNotAvailableAnymoreException("Sample clips are not supported");
        }
        if (getContentAsString().contains("Media not available")) {
            throw new URLNotAvailableAnymoreException("Media not available");
        }
    }

    private void setSwfVerification(final RtmpSession session, final String swfUrl) throws Exception {
        synchronized (CrunchyRollFileRunner.class) {
            if (helper == null || !helper.getSwfURL().equals(swfUrl)) {
                logger.info("New SWF URL: " + swfUrl);
                helper = new SwfVerificationHelper(swfUrl);
            }
            helper.setSwfVerification(session, client);
        }
    }

}