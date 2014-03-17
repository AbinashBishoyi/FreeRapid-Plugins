package cz.vity.freerapid.plugins.services.paid4share;

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
 * @author Vity
 * @since 0.82
 */
class Paid4ShareRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(Paid4ShareRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkSeriousProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws URLNotAvailableAnymoreException {
        if (getContentAsString().contains("was not found")) {
            throw new URLNotAvailableAnymoreException("File was not found");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
            final HttpMethod m = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download file").toHttpMethod();

            if (!tryDownloadAndSaveFile(m)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty");
            }
        } else {
            checkProblems();
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("You have got max allowed download sessions from the same IP")) {
            throw new YouHaveToWaitException("You have got max allowed download sessions from the same IP!", 60);
        }

    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<td align=left width=150px>", "</td>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "File size:</b></td>\n<td align=left>", "</td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

}
