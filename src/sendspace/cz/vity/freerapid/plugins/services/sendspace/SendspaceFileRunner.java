package cz.vity.freerapid.plugins.services.sendspace;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda
 * @author ntoskrnl
 * @since 0.82
 */
class SendspaceFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(SendspaceFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkSeriousProblems();
            checkNameAndSize();
        } else {
            checkSeriousProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkAllProblems();
            checkNameAndSize();
            if (getContentAsString().contains("quickdownloadbutton")) {
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(fileURL)
                        .setParameter("quickdownloadbutton", "")
                        .toPostMethod();
                if (!makeRedirectedRequest(method)) {
                    checkAllProblems();
                    throw new ServiceConnectionProblemException();
                }
            }
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("start download")
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkAllProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkAllProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("404 Page Not Found")
                || contentAsString.contains("Sorry, the file you requested is not available")
                || contentAsString.contains("The page you are looking for is  not available")) {
            throw new URLNotAvailableAnymoreException("File was not found");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("You cannot download more than one file at a time")) {
            throw new YouHaveToWaitException("You cannot download more than one file at a time", 60);
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        final Matcher matcher = getMatcherAgainstContent("<h2 class=\"bgray\"><(?:b|strong)>(.+?)</");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(matcher.group(1));
        PlugUtils.checkFileSize(httpFile, contentAsString, "Size:</b>", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

}