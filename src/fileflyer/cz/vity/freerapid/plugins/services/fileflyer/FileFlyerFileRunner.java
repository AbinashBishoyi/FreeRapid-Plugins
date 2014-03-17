package cz.vity.freerapid.plugins.services.fileflyer;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda
 */
class FileFlyerFileRunner extends AbstractRunner {
    private static final Logger LOGGER = Logger.getLogger(FileFlyerFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkSeriousProblems();
            checkNameAndSize();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        LOGGER.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkAllProblems();
            checkNameAndSize();

            final Matcher matcher = getMatcherAgainstContent("class=\"handlink\" href=\"(.+?)\"");

            if (matcher.find()) {
                client.setReferer(fileURL);
                final String finalURL = matcher.group(1);
                getMethod = getGetMethod(finalURL);

                if (!tryDownloadAndSaveFile(getMethod)) {
                    checkAllProblems();
                    LOGGER.warning(getContentAsString());
                    throw new IOException("File input stream is empty");
                }
            } else {
                throw new PluginImplementationException("Download link was not found");
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("To report a bug")) {
            throw new URLNotAvailableAnymoreException("This file does not exist or has been removed");
        }

        if (contentAsString.contains("Expired")) {
            throw new URLNotAvailableAnymoreException("Expired");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("Please try again later")) {
            throw new YouHaveToWaitException("Due to FileFlyer server loads in your area, access to the service may be unavailable for a while. Please try again later", 60);
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("id=\"ItemsList_ctl00_file\".+?>(.+?)<");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();
            LOGGER.info("File name " + fileName);
            httpFile.setFileName(fileName);

            matcher = getMatcherAgainstContent("id=\"ItemsList_ctl00_size\">(.+?)<");

            if (matcher.find()) {
                final long fileSize = PlugUtils.getFileSizeFromString(matcher.group(1));
                LOGGER.info("File size " + fileSize);
                httpFile.setFileSize(fileSize);
            } else {
                LOGGER.warning("File size was not found");
                throw new PluginImplementationException();
            }
        } else {
            LOGGER.warning("File name was not found");
            throw new PluginImplementationException();
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}