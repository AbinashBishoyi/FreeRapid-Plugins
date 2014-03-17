package cz.vity.freerapid.plugins.services.sharingmatrix;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
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
class SharingMatrixFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SharingMatrixFileRunner.class.getName());

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
            String contentAsString = getContentAsString();
            final String rootURL = PlugUtils.getStringBetween(contentAsString, "URL_ROOT = '", "'");
            String redirectURL = rootURL + "/ajax_scripts/_get.php?link_id=" + PlugUtils.getStringBetween(contentAsString, "link_id = '", "'") + "&link_name=" + PlugUtils.getStringBetween(contentAsString, "link_name = '", "'") + "&dl_id=0&password=";
            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/ajax_scripts/dl.php").toHttpMethod();

            if (makeRedirectedRequest(httpMethod)) {
                final String dlID = getContentAsString();
                redirectURL = redirectURL.replaceFirst("dl_id=0", "dl_id=" + dlID);
                httpMethod = getMethodBuilder().setReferer(fileURL).setAction(redirectURL).toHttpMethod();

                if (makeRedirectedRequest(httpMethod)) {
                    contentAsString = getContentAsString();
                    httpMethod = getMethodBuilder().setReferer(fileURL).setAction("/download/" + PlugUtils.getStringBetween(contentAsString, "hash:\"", "\"") + "/" + dlID + "/").setBaseURL(PlugUtils.getStringBetween(contentAsString, "{serv:\"", "\"")).toHttpMethod();

                    if (!tryDownloadAndSaveFile(httpMethod)) {
                        checkAllProblems();
                        logger.warning(getContentAsString());
                        throw new IOException("File input stream is empty");
                    }
                } else {
                    throw new ServiceConnectionProblemException();
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
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString().replaceAll("&nbsp;", " ");
        PlugUtils.checkName(httpFile, contentAsString, "Filename:</th>\n<td>", "<");
        PlugUtils.checkFileSize(httpFile, contentAsString, "Size:</th>\n<td>", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}