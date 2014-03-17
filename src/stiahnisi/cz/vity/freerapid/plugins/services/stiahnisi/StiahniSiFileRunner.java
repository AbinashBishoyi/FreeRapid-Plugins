package cz.vity.freerapid.plugins.services.stiahnisi;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class StiahniSiFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(StiahniSiFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h1 title=\"", "\"");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(PlugUtils.getStringBetween(content, "Veľkosť</td>", "</td>").replace("<td class=\"value\">", "")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkFileProblems();
            checkNameAndSize(contentAsString);
            checkDownloadProblems();
            final String[] waitTimeArray = PlugUtils.getStringBetween(getContentAsString(), "var limit=\"", "\"").split(":");
            downloadTask.sleep(Integer.parseInt(waitTimeArray[0]) * 60 + Integer.parseInt(waitTimeArray[1]) + 1);
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromTextBetween("window.location='", "';")
                    .toHttpMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkDownloadProblems();
                logger.info(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("bol zmazaný")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Všetky free sloty sú obsadené")) {
            throw new YouHaveToWaitException("All free slots are occupied", 5 * 60);
        }
        if (contentAsString.contains("Paralelné sťahovanie nieje pre free uzivateľov povolené")) {
            throw new PluginImplementationException("Parallel download for free users is not allowed");
        }
    }

}