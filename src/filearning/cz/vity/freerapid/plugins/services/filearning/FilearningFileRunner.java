package cz.vity.freerapid.plugins.services.filearning;

import cz.vity.freerapid.plugins.exceptions.*;
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
 * @author Tommy
 */
class FilearningFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilearningFileRunner.class.getName());
    private boolean isDownloadBegin = false;

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        isDownloadBegin = false;
        replaceFileUrl();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        Matcher matcher = PlugUtils.matcher("\"(.+?)\" \\(([0-9\\.]+ [KMG]?B)\\)", content);
        if (!matcher.find())
            throw new PluginImplementationException("Couldn't find file name and size.");

        httpFile.setFileName(matcher.group(1));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        isDownloadBegin = false;
        replaceFileUrl();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            final HttpMethod httpMethod = downloadWaitPage(contentAsString);
            isDownloadBegin = true;

            setFileStreamContentTypes("\"application/octet-stream\"");
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        } else if (isDownloadBegin && contentAsString.contains("var waitSecs = ")) {
            String strWait = PlugUtils.getStringBetween(contentAsString, "var waitSecs = ", ";");
            int wait = Integer.parseInt(strWait);
            throw new YouHaveToWaitException("Please wait...", wait);
        }
    }

    private void replaceFileUrl() {
        fileURL = fileURL.replaceAll("/files/", "/get/");
    }

    private HttpMethod downloadWaitPage(String contentAsString) throws PluginImplementationException, InterruptedException {
        String strTimestamp = PlugUtils.getStringBetween(contentAsString, "waitSecs, '", "'");
        final String fid = getFileId(contentAsString);
        final String downUrl = String.format("http://www.filearning.com/get/%s/%s.html",
                fid, strTimestamp);

        return getMethodBuilder()
                .setReferer(fileURL)
                .setAction(downUrl)
                .setParameter("task", "download")
                .setParameter("time", strTimestamp)
                .toPostMethod();
    }

    private String getFileId(String content) throws PluginImplementationException {
        return PlugUtils.getStringBetween(content, "file_id = '", "';");
    }

}