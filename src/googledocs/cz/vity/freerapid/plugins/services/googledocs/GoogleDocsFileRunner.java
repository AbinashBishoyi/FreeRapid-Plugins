package cz.vity.freerapid.plugins.services.googledocs;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u3
 */
class GoogleDocsFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GoogleDocsFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "\"filename\":\"", "\"");
        PlugUtils.checkFileSize(httpFile, content, "\"fileSize\":\"", "\"");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        if (!fileURL.contains("confirm=no_antivirus")) {
            if (!fileURL.contains("?")) {
                fileURL += "?confirm=no_antivirus";
            } else {
                fileURL += "&confirm=no_antivirus";
            }
        }
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            String downloadUrl;
            try {
                downloadUrl = PlugUtils.unescapeUnicode(PlugUtils.getStringBetween(getContentAsString(), "\"downloadUrl\":\"", "\""));
            } catch (PluginImplementationException e) {
                throw new PluginImplementationException("Download URL not found");
            }
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(downloadUrl).toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            String referer = httpMethod.getURI().toString();
            URL url = new URL(referer);
            Matcher matcher = getMatcherAgainstContent("<a id=\"uc-download-link\".*? href=\"(.+?)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Download link not found");
            }
            String downloadLink = PlugUtils.replaceEntities(URLDecoder.decode(matcher.group(1).trim(), "UTF-8"));
            String baseUrl = url.getProtocol() + "://" + url.getAuthority();
            httpMethod = getMethodBuilder()
                    .setReferer(referer)
                    .setBaseURL(baseUrl)
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

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file you have requested does not exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}
