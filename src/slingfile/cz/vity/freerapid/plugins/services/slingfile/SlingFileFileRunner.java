package cz.vity.freerapid.plugins.services.slingfile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/*
 * @author TommyTom
 */
class SlingFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SlingFileFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        if (fileURL.contains("/dl/"))
            fileURL = fileURL.replaceFirst("/dl/", "/file/");
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

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "<title>", " - SlingFile");
        PlugUtils.checkFileSize(httpFile, content, "Size:</strong>", "</li>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        if (fileURL.contains("/dl/"))
            fileURL = fileURL.replaceFirst("/dl/", "/file/");
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setBaseURL(fileURL)
                    .setActionFromFormByName("download_form", true)
                    .toPostMethod();
            if (makeRedirectedRequest(httpMethod)) {
                httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("here").toGetMethod();
                //downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "var seconds=", ";") + 1);
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("The file you requested cannot be found") ||
                content.contains("The file you have requested was not found") ||
                content.contains("The file you have requested has been deleted") ||
                content.contains("<h1>Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("Resume feature is not allowed for Free/Anonymous users"))
            throw new NotRecoverableDownloadException("Resume feature is not allowed for Free/Anonymous users");
    }

}