package cz.vity.freerapid.plugins.services.toshared;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * @author Tiago Hillebrandt <tiagohillebrandt@gmail.com>, ntoskrnl
 */
class ToSharedRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ToSharedRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();

        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();

        logger.info("Starting download is TASK " + fileURL);

        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();

            final String downloadURL = PlugUtils.getStringBetween(getContentAsString(), "window.location = \"", "\";");

            final GetMethod method = getGetMethod(downloadURL);

            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        PlugUtils.checkName(httpFile, getContentAsString(), "download", "</title>");

        final String fileSize = PlugUtils.getStringBetween(getContentAsString(), "File size:</span>\n", "&nbsp;");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize.replace(",", "")));

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("The file link that you requested is not valid")) {
            throw new URLNotAvailableAnymoreException("The file link that you requested is not valid");
        }
        if (content.contains("User downloading session limit is reached")) {
            throw new ServiceConnectionProblemException("Your IP address is already downloading a file or your session limit is reached! Try again in a few minutes.");
        }
    }
}
