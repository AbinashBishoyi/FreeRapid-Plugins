package cz.vity.freerapid.plugins.services.triluliluro;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class TriluliluRoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TriluliluRoFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            isAtHomePage(getMethod);
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h1>", "</h1>");
        httpFile.setFileName(httpFile.getFileName() + "." + "mp4");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            isAtHomePage(method);
            checkProblems();
            checkNameAndSize(contentAsString);
            final String type = "video";
            final String source = "site";
            final String userId = PlugUtils.getStringBetween(getContentAsString(), "\"userid\":\"", "\"");
            final String hash = PlugUtils.getStringBetween(getContentAsString(), "\"hash\":\"", "\"");
            final String server = PlugUtils.getStringBetween(getContentAsString(), "\"server\":\"", "\"");
            String key = PlugUtils.getStringBetween(getContentAsString(), "\"key\":\"", "\"");
            if (key.equals("")) key = "ministhebest";
            final String start = "";
            final String referer = PlugUtils.getStringBetween(getContentAsString(), "embedSWF(\"", "\"");
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(referer)
                    .setAction(String.format("http://fs%s.trilulilu.ro/%s/video-formats2", server, hash))
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final String format = PlugUtils.getStringBetween(getContentAsString(), "<format>", "</format>");
            httpMethod = getMethodBuilder()
                    .setReferer(referer)
                    .setAction(String.format("http://fs%s.trilulilu.ro/stream.php?type=%s&source=%s&hash=%s&username=%s&key=%s&format=%s&start=%s", server, type, source, hash, userId, key, format, start))
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
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void isAtHomePage(final HttpMethod method) throws URIException, URLNotAvailableAnymoreException {
        if (method.getURI().toString().matches("http://(?:www\\.)?trilulilu\\.ro/?")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}