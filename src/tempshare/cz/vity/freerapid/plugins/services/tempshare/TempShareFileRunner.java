package cz.vity.freerapid.plugins.services.tempshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class TempShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TempShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            isAtHomePage(getMethod);
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h1>", "</h1>");
        PlugUtils.checkFileSize(httpFile, content, "<h2>", "</h2>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            isAtHomePage(method);
            checkNameAndSize(contentAsString);
            final Matcher downloadLinkMatcher = getMatcherAgainstContent("<a href=[\"'](/download/.+?)[\"']");
            if (!downloadLinkMatcher.find()) {
                throw new PluginImplementationException("Download link not found");
            }
            final String downloadLink = downloadLinkMatcher.group(1);
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadLink)
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void isAtHomePage(HttpMethod httpMethod) throws Exception {
        if (httpMethod.getURI().toString().equals("http://temp-share.com/home")
                || httpMethod.getURI().toString().equals("http://temp-share.com")
                || httpMethod.getURI().toString().equals("http://temp-share.com/")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Time has expired for this file")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}