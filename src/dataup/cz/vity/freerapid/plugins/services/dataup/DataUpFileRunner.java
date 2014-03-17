package cz.vity.freerapid.plugins.services.dataup;

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
class DataUpFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DataUpFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final MethodBuilder builder = getMethodBuilder();
        final HttpMethod httpMethod = builder.setAction(fileURL).encodeLastPartOfAction().toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
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
        HttpMethod httpMethod = builder.setAction(fileURL).encodeLastPartOfAction().toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();

            builder = getMethodBuilder();
            builder.setActionFromFormByName("dl_load", true);
            httpMethod = builder.setReferer(builder.getAction()).toHttpMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty");
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("Datei nicht gefunden")) {
            throw new URLNotAvailableAnymoreException("Datei nicht gefunden");
        }

        if (contentAsString.contains("Es wurde kein Download unter diesem Link gefunden")) {
            throw new URLNotAvailableAnymoreException("Es wurde kein Download unter diesem Link gefunden");
        }

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
