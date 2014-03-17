package cz.vity.freerapid.plugins.services.svtplay;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.adobehds.HdsDownloader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class SvtPlayFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SvtPlayFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkName() throws ErrorDuringDownloadingException {
        final String name = PlugUtils.getStringBetween(getContentAsString(), "<title>", "|");
        httpFile.setFileName(PlugUtils.unescapeHtml(name) + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
            method = getMethodBuilder().setAction("/video/" + getIdFromUrl() + "?output=json").toGetMethod();
            if (makeRedirectedRequest(method)) {
                final Matcher matcher = getMatcherAgainstContent("\"url\":\"([^\"]+)\",\"bitrate\":0,\"playerType\":\"flash\"");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Manifest URL not found");
                }
                final HdsDownloader downloader = new HdsDownloader(client, httpFile, downloadTask);
                downloader.tryDownloadAndSaveFile(matcher.group(1) + "?hdcore=2.11.3&g=DEFSPICMJSJJ");
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("sidan finns inte")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String getIdFromUrl() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("/video/(\\d+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        return matcher.group(1);
    }

}