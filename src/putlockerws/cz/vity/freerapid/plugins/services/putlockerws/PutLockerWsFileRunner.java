package cz.vity.freerapid.plugins.services.putlockerws;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u3
 */
class PutLockerWsFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PutLockerWsFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkUrl();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkUrl() {
        if (!fileURL.startsWith("http://www.")) {
            fileURL = fileURL.replaceFirst("http://", "http://www.");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("<h1>\\s*?(.+?)\\s*?<strong>\\((.+?)\\)</strong>\\s*?</h1>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name/size not found");
        }
        String filename = matcher.group(1).trim();
        long filesize = PlugUtils.getFileSizeFromString(matcher.group(2).trim());
        logger.info("File name : " + filename);
        logger.info("File size : " + filesize);
        httpFile.setFileName(filename);
        httpFile.setFileSize(filesize);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkUrl();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("confirm", true)
                    .setAction(fileURL)
                    .setParameter("confirm", "Continue as Free User")
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            String downloadURL;
            downloadURL = PlugUtils.getStringBetween(getContentAsString(), "url: '", "'");
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadURL)
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file you were looking for could not be found")
                || contentAsString.contains("404 Not Found")
                || contentAsString.contains("file was deleted by its owner")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("Server is Overloaded")) {
            throw new YouHaveToWaitException("Server is overloaded", 60 * 2);
        }
    }

}
