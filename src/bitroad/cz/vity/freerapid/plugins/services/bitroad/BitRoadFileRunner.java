package cz.vity.freerapid.plugins.services.bitroad;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Kajda
 * @since 0.82
 */
class BitRoadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BitRoadFileRunner.class.getName());
    private final static String SERVICE_WEB = "http://bitroad.net";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setPageEncoding("Windows-1251");
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
        setPageEncoding("Windows-1251");
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkNameAndSize();
            httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("Premium", true).setBaseURL(SERVICE_WEB).toHttpMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkAllProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty");
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("The requested file is not found")) {
            throw new URLNotAvailableAnymoreException("The requested file is not found");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("Downloading is in process from your IP-Address")) {
            throw new YouHaveToWaitException("Downloading is in process from your IP-Address", 60);
        }

        if (contentAsString.contains("You already download files from current IP")) {
            throw new YouHaveToWaitException("You already download files from current IP. Please wait some time for download another file", 4);
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "class=\"content_text\"><h1>", " [");
        PlugUtils.checkFileSize(httpFile, contentAsString, "[ ", " ]</h1>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}