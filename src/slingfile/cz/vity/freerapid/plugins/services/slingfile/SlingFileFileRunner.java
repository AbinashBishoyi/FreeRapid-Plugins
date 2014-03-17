package cz.vity.freerapid.plugins.services.slingfile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
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
        PlugUtils.checkName(httpFile, content, "File Name: <strong>", "</strong></p>");
        PlugUtils.checkFileSize(httpFile, content, "File Size: ", "</p>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setActionFromFormByName("form1", true).toPostMethod();
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
        if (content.contains("The file you requested cannot be found") || content.contains("<h1>Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}