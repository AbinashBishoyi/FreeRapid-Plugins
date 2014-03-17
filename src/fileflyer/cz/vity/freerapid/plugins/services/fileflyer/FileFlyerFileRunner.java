package cz.vity.freerapid.plugins.services.fileflyer;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * @author Kajda
 * @author tong2shot
 */
class FileFlyerFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(FileFlyerFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
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
        GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkAllProblems();
            checkNameAndSize();
            final HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("Download")
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkAllProblems();
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("To report a bug")) {
            throw new URLNotAvailableAnymoreException("This file does not exist or has been removed");
        }
        if (contentAsString.contains("Expired")) {
            throw new URLNotAvailableAnymoreException("Expired");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Please try again later")) {
            throw new YouHaveToWaitException("Due to FileFlyer server loads in your area, access to the service may be unavailable for a while. Please try again later", 60);
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "ItemsList_ctl00_file\" title=\"", "\"");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "ItemsList_ctl00_size\">", "</");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}