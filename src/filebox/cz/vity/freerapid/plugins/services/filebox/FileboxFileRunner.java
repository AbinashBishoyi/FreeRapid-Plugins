package cz.vity.freerapid.plugins.services.filebox;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
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
class FileboxFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileboxFileRunner.class.getName());

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
            downloadTask.sleep(PlugUtils.getWaitTimeBetween(getContentAsString(), "<span id=\"countdown\">", "<", TimeUnit.SECONDS));
            httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("F1", true).setBaseURL(fileURL).toHttpMethod();

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

        if (contentAsString.contains("No such user exist")) {
            throw new URLNotAvailableAnymoreException("No such user exist");
        }

        if (contentAsString.contains("No such file from this user")) {
            throw new URLNotAvailableAnymoreException("No such file from this user");
        }

        if (contentAsString.contains("No such file")) {
            throw new URLNotAvailableAnymoreException("No such file");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "Filename:</b></td>\n<td nowrap>", "<");
        PlugUtils.checkFileSize(httpFile, contentAsString, "Size:</b></td>\n<td>", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}