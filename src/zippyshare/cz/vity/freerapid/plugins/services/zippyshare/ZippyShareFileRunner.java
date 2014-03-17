package cz.vity.freerapid.plugins.services.zippyshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * @author Kajda
 * @since 0.82
 */
class ZippyShareFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(ZippyShareFileRunner.class.getName());

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
            final String contentAsString = getContentAsString();
            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(URLDecoder.decode(PlugUtils.getStringBetween(contentAsString, "var comeonguys = '", "';"), "UTF-8").substring(PlugUtils.getNumberBetween(contentAsString, "substring(", ");"))).toHttpMethod();

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

        if (contentAsString.contains("The requsted file does not exist on this server")) {
            throw new URLNotAvailableAnymoreException("The requsted file does not exist on this server");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "Name: </strong>", "<");
        PlugUtils.checkFileSize(httpFile, contentAsString, "Size: </strong>", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}