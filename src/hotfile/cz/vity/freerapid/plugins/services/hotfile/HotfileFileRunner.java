package cz.vity.freerapid.plugins.services.hotfile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda
 */
class HotfileFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(HotfileFileRunner.class.getName());
    private static final String SERVICE_WEB = "http://hotfile.com";

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
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

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

        if (contentAsString.isEmpty() || contentAsString.contains("404 - Not Found")) {
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
        final int waitTime = PlugUtils.getWaitTimeBetween(getContentAsString(), "", "document.getElementById('dwltxt", TimeUnit.MILLISECONDS);
        downloadTask.sleep(waitTime);

        if (makeRedirectedRequest(httpMethod)) {
            downloadFile();
        } else {
            throw new ServiceConnectionProblemException();
        }
    }

    private void downloadFile() throws Exception {
        final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Click here to download").toHttpMethod();

        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkAllProblems();
            logger.warning(getContentAsString());
            throw new IOException("File input stream is empty");
        }
    }
}
