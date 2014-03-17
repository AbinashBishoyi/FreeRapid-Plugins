package cz.vity.freerapid.plugins.services.radiouolcom;

import cz.vity.freerapid.plugins.exceptions.BuildMethodException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u2
 */
class RadioUolComFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RadioUolComFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final String mediaId = getMediaIdFromUrl();
        final HttpMethod method = getMediaInfoMethod(mediaId);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String filename = PlugUtils.getStringBetween(getContentAsString(), "\"title\": \"", "\"").trim();
        if (filename.isEmpty()) throw new URLNotAvailableAnymoreException("Media not found");
        httpFile.setFileName(filename + ".mp3");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final String mediaId = getMediaIdFromUrl();
        HttpMethod method = getMediaInfoMethod(mediaId);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(String.format("http://storage.mais.uol.com.br/%s.mp3", mediaId))
                    .toHttpMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private HttpMethod getMediaInfoMethod(String mediaId) throws BuildMethodException {
        return getMethodBuilder()
                .setReferer(fileURL)
                .setAction("http://mais.uol.com.br/apiuol/player/media.js")
                .setParameter("mediaId", mediaId)
                .setParameter("p", "mais")
                .setParameter("action", "showPlayer")
                .setParameter("types", "P")
                .setParameter("callback", "setVideoInfo")
                .toGetMethod();
    }

    private String getMediaIdFromUrl() {
        return fileURL.substring(fileURL.lastIndexOf("/") + 1);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}