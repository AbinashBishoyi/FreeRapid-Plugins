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
        setLang();
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
        PlugUtils.checkName(httpFile, content, "name\">", "</");
        PlugUtils.checkFileSize(httpFile, content, "fileSize\" content=\"", "\"/>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void setLang() {
        if (PlugUtils.matcher("stiahni.si/\\w+?/file", fileURL).find())
            fileURL = fileURL.replaceFirst("stiahni.si/\\w+?/file", "stiahni.si/en/file");
        else
            fileURL = fileURL.replaceFirst("stiahni.si/file", "stiahni.si/en/file");
    }

    @Override
    public void run() throws Exception {
        setLang();
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkFileProblems();
            checkNameAndSize(contentAsString);
            checkDownloadProblems();
            final int waitTime = PlugUtils.getNumberBetween(getContentAsString(), "var parselimit =", ";");
            downloadTask.sleep(waitTime + 1);
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
        if (contentAsString.contains("bol zmazaný") ||
                contentAsString.contains("Soubor nikdo nestáhnul více") ||
                contentAsString.contains("Soubor obsahoval nelegální obsah") ||
                contentAsString.contains("Soubor byl smazaný uploaderem") ||
                contentAsString.contains("This file does not exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Všetky free sloty sú obsadené") ||
                contentAsString.contains("Všechny free sloty jsou obsazené")) {
            throw new YouHaveToWaitException("All free slots are occupied", 5 * 60);
        }
        if (contentAsString.contains("Paralelné sťahovanie nieje pre free uzivateľov povolené") ||
                contentAsString.contains("Free download dont support parallel downloading")) {
            throw new PluginImplementationException("Parallel download for free users is not allowed");
        }
    }

}