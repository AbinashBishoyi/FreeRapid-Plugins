package cz.vity.freerapid.plugins.services.dataup;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda
 * @since 0.82
 */
class DataUpFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(DataUpFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setPageEncoding("ISO-8859-1");
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).setEncodePathAndQuery(true).toHttpMethod();

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
        setPageEncoding("ISO-8859-1");
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).setEncodePathAndQuery(true).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkNameAndSize();
            httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("dl_load", true).toHttpMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkAllProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty");
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("Datei nicht gefunden")) {
            throw new URLNotAvailableAnymoreException("Datei nicht gefunden");
        }

        if (contentAsString.contains("Es wurde kein Download unter diesem Link gefunden")) {
            throw new URLNotAvailableAnymoreException("Es wurde kein Download unter diesem Link gefunden");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();

        final Matcher matcher = getMatcherAgainstContent("Der Download Link ist aus Sicherheitsgr.nden nur 5 Minuten Aktiv");

        if (matcher.find()) {
            throw new YouHaveToWaitException("Der Download Link ist aus Sicherheitsgründen nur 5 Minuten Aktiv. Fordere den Download bitte neu an", 10);
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("style=\"font: 11px verdana, arial, helvetica;\">(.+?)<");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

            matcher = getMatcherAgainstContent("label>Gr..e: (.+?)<");

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
}
