package cz.vity.freerapid.plugins.services.hotfile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.io.IOException;

/**
 * @author Kajda
 */
class HotfileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(HotfileFileRunner.class.getName());
    private final static String SERVICE_WEB = "http://hotfile.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final MethodBuilder builder = getMethodBuilder();
        final HttpMethod httpMethod = builder.setAction(fileURL).toHttpMethod();

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
        MethodBuilder builder = getMethodBuilder();
        HttpMethod httpMethod = builder.setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkNameAndSize();

            if (getContentAsString().contains("var timerend=0;")) {
                processDownloadWithForm();
            } else {
                downloadFile();
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.isEmpty()) {
            throw new URLNotAvailableAnymoreException("File was not found");
        }

        if (contentAsString.contains("404 - Not Found")) {
            throw new URLNotAvailableAnymoreException("File was not found");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("Your download expired")) {
            throw new YouHaveToWaitException("Your download expired", 60);
        }

        final Matcher matcher = getMatcherAgainstContent("([0-9]+?);\\s*document.getElementById\\('dwltmr");

        if (matcher.find()) {
            final int waitTime = Integer.parseInt(matcher.group(1)) / 1000;

            if (waitTime > 0) {
                throw new YouHaveToWaitException("You reached your hourly traffic limit", waitTime);
            }
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("20px'>Downloading (.+?) \\(");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

            matcher = getMatcherAgainstContent("\\((.+?)\\)</h2><(?:h3|script)");

            if (matcher.find()) {
                final long fileSize = PlugUtils.getFileSizeFromString(matcher.group(1));
                logger.info("File size " + fileSize);
                httpFile.setFileSize(fileSize);
            } else {
                logger.warning("File size was not found");
                throw new PluginImplementationException();
            }
        } else {
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void processDownloadWithForm() throws Exception {
        final MethodBuilder builder = getMethodBuilder();
        builder.setActionFromFormByName("f", true);
        final HttpMethod httpMethod = builder.setReferer(fileURL).setBaseURL(SERVICE_WEB).toHttpMethod();
        downloadTask.sleep(getWaitTime());

        if (makeRedirectedRequest(httpMethod)) {
            downloadFile();
        } else {
            throw new ServiceConnectionProblemException();
        }
    }

    private int getWaitTime() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("([0-9]+?);\\s*document.getElementById\\('dwltxt");

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) / 1000;
        } else {
            throw new PluginImplementationException("Wait time value was not found");
        }
    }

    private void downloadFile() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("href=\"(.+?)\">Click here to download");

        if (matcher.find()) {
            final String finalURL = matcher.group(1);
            final MethodBuilder builder = getMethodBuilder();
            final HttpMethod httpMethod = builder.setReferer(fileURL).setAction(finalURL).toHttpMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkAllProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty");
            }
        } else {
            throw new PluginImplementationException("Download link was not found");
        }
    }
}
