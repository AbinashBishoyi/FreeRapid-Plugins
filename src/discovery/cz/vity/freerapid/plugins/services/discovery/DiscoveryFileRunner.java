package cz.vity.freerapid.plugins.services.discovery;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class DiscoveryFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DiscoveryFileRunner.class.getName());
    private final static String SWF_URL = "http://player.video.dp.discovery.com/video-asset-page-player.swf";

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

    private void checkNameAndSize() throws Exception {
        final String program = PlugUtils.getStringBetween(getContentAsString(), "\"programTitle\": \"", "\"");
        final String episode = PlugUtils.getStringBetween(getContentAsString(), "\"episodeTitle\": \"", "\"");
        httpFile.setFileName(program + " - " + episode + ".flv");
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
            final String id = PlugUtils.getStringBetween(getContentAsString(), "\"clipRefId\": \"", "\"");
            method = getMethodBuilder()
                    .setReferer(SWF_URL)
                    .setAction("http://static.discoverymedia.com/videos/components/dsc/" + id + "/smil-service.smil")
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final String video = PlugUtils.getStringBetween(getContentAsString(), "<video src=\"", "\"");
            method = getMethodBuilder()
                    .setReferer(SWF_URL)
                    .setAction("http://discidevflash-f.akamaihd.net/" + video)
                    .toGetMethod();
            //they always send Content-Length: 2147483647
            setClientParameter(DownloadClientConsts.NO_CONTENT_LENGTH_AVAILABLE, true);
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("We're sorry, but the page you requested")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}