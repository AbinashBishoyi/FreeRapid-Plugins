package cz.vity.freerapid.plugins.services.fileupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

/**
 * @author Kajda
 * @since 0.82
 */
class FileUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileUploadFileRunner.class.getName());
    private final static String SERVICE_HOST = "www.file-upload.eu";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setPageEncoding("ISO-8859-1");
        fileURL = checkFileURL(fileURL);
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
        setPageEncoding("ISO-8859-1");
        fileURL = checkFileURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkNameAndSize();
            httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormWhereActionContains("/data.php?id=", true).toHttpMethod();

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

        if (contentAsString.contains("This file has been deleted") || contentAsString.contains("This file does not exist on our server")) {
            throw new URLNotAvailableAnymoreException("This file has been deleted");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "<h1>Download \"", "\"<");
        PlugUtils.checkFileSize(httpFile, contentAsString, "Filesize:</b></td><td> ", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private String checkFileURL(String fileURL) throws URISyntaxException {
        final URI fileURI = new URI(fileURL);
        final String host = fileURI.getHost();

        if (!host.equalsIgnoreCase(SERVICE_HOST)) {
            fileURL = fileURL.replaceFirst(host, SERVICE_HOST);
        }

        return fileURL;
    }
}