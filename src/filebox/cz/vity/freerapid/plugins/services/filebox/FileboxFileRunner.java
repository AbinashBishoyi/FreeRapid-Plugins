package cz.vity.freerapid.plugins.services.filebox;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda
 * @since 0.82
 */
class FileboxFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileboxFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkSeriousProblems();
            final Matcher waitMatcher = getMatcherAgainstContent("Wait.*>(\\d*)<.*seconds");
            if (waitMatcher.find()) {
                downloadTask.sleep(Integer.parseInt(waitMatcher.group(1)));
            }
            String contentAsString = getContentAsString();
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL).setAction(fileURL)
                    .setParameter("op", PlugUtils.getStringBetween(contentAsString, "name=\"op\" value=\"", "\">"))
                    .setParameter("id", PlugUtils.getStringBetween(contentAsString, "name=\"id\" value=\"", "\">"))
                    .setParameter("rand", PlugUtils.getStringBetween(contentAsString, "name=\"rand\" value=\"", "\">"))
                    .setParameter("method_free", "1")
                    .setParameter("down_direct", "1")
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkAllProblems();
                throw new PluginImplementationException("Problem loading page");
            }
            checkAllProblems();
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
            checkSeriousProblems();

            final Matcher waitMatcher = getMatcherAgainstContent("Wait.*>(\\d*)<.*seconds");
            if (waitMatcher.find()) {
                downloadTask.sleep(Integer.parseInt(waitMatcher.group(1)));
            }
            String contentAsString = getContentAsString();
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL).setAction(fileURL)
                    .setParameter("op", PlugUtils.getStringBetween(contentAsString, "name=\"op\" value=\"", "\">"))
                    .setParameter("id", PlugUtils.getStringBetween(contentAsString, "name=\"id\" value=\"", "\">"))
                    .setParameter("rand", PlugUtils.getStringBetween(contentAsString, "name=\"rand\" value=\"", "\">"))
                    .setParameter("method_free", "1")
                    .setParameter("down_direct", "1")
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkAllProblems();
                throw new PluginImplementationException("Problem loading page");
            }
            checkAllProblems();
            checkNameAndSize();

            String url = PlugUtils.getStringBetween(getContentAsString(), "product_download_url=", "\"");
            httpMethod = getGetMethod(url);
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkAllProblems();
                throw new ServiceConnectionProblemException("Error starting download");
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
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File Not Found");
        }

        if (contentAsString.contains("No such file from this user")) {
            throw new URLNotAvailableAnymoreException("No such file from this user");
        }

        if (contentAsString.contains("No such file") || contentAsString.contains("contentAsString.contains(\"This Link Is Not Available\")")) {
            throw new URLNotAvailableAnymoreException("No such file");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "File Name : <span>", "</span>");
        PlugUtils.checkFileSize(httpFile, contentAsString, "Size : <span class=\"txt2\">", "</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}