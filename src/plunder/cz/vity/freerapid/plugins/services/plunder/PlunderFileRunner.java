package cz.vity.freerapid.plugins.services.plunder;

import cz.vity.freerapid.plugins.exceptions.*;
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
class PlunderFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PlunderFileRunner.class.getName());

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
            //httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download").toHttpMethod();
            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(PlugUtils.getStringBetween(getContentAsString(), "\"", "\">Download").replaceAll(" ", "%20")).toHttpMethod();

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

        if (contentAsString.contains("File not found") || contentAsString.contains("An unexpected error has occured")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();

        if (contentAsString.isEmpty() || contentAsString.contains("Bad Request")) {
            throw new PluginImplementationException("Download link is incorrect");
        }

        if (contentAsString.contains("You must log in to download more this session")) {
            throw new YouHaveToWaitException("You must log in to download more this session", 60);
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "Filename: </b>", "(");
        PlugUtils.checkFileSize(httpFile, contentAsString, "(", ")<BR /><a href=\"");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}