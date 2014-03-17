package cz.vity.freerapid.plugins.services.nahraj;

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
class NahrajFileRunner extends AbstractRunner {
    private static final Logger LOGGER = Logger.getLogger(NahrajFileRunner.class.getName());

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

            final Matcher matcher = getMatcherAgainstContent("enctype=\"multipart/form-data\" action=\"(.+?)\"");

            if (matcher.find()) {
                client.setReferer(fileURL);
                final String finalURL = matcher.group(1);
                //client.getHTTPClient().getParams().setParameter("considerAsStream", "text/plain");
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
        final Matcher matcher = getMatcherAgainstContent("Neznam. soubor");

        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("Neznamý soubor");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();

        final Matcher matcher = getMatcherAgainstContent("V.cen.sobn. download");

        if (matcher.find()) {
            throw new YouHaveToWaitException("Vícenásobný download", 60);
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("class=\"title\">(.+?)<");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();
            LOGGER.info("File name " + fileName);
            httpFile.setFileName(fileName);

            matcher = getMatcherAgainstContent("class=\"size\">(.+?)<");

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
