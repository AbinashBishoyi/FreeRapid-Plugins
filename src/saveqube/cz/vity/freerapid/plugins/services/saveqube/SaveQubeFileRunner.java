package cz.vity.freerapid.plugins.services.saveqube;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Kajda
 * @since 0.82
 */
class SaveQubeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SaveQubeFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkSeriousProblems();
            checkNameAndSize();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkNameAndSize();

            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("free", "").toPostMethod();

            if (makeRedirectedRequest(httpMethod)) {
                final String contentAsString = getContentAsString();
                downloadTask.sleep(PlugUtils.getWaitTimeBetween(contentAsString, "span id=\"timer\">", "<", TimeUnit.SECONDS));
                httpMethod = getMethodBuilder().setReferer(fileURL).setAction(PlugUtils.getStringBetween(contentAsString, "href=\"", "\" id=\"freedownload\">")).toHttpMethod();

                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkAllProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty");
                }
            } else {
                throw new ServiceConnectionProblemException();
            }

        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("bot detected")) {
            throw new PluginImplementationException("bot detected");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "strong class=\"file\">", "<");
        PlugUtils.checkFileSize(httpFile, contentAsString, "span class=\"file2\">File size: ", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}