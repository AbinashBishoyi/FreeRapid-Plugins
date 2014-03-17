package cz.vity.freerapid.plugins.services.putlocker;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class PutLockerFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PutLockerFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
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
        PlugUtils.checkName(httpFile, content, "<h1>", "<strong>");
        PlugUtils.checkFileSize(httpFile, content, "<strong>( ", " )</strong></h1>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            if (contentAsString.contains("countdownNum")) {
                final int waitTime = PlugUtils.getWaitTimeBetween(contentAsString, "var countdownNum = ", ";", TimeUnit.SECONDS);
                downloadTask.sleep(waitTime);
            }

            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("confirm", true)
                    .setAction(fileURL)
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new PluginImplementationException();
            }
            contentAsString = getContentAsString();
            checkProblems();

            final String downloadURL;
            boolean isVideoStream = false;
            if (contentAsString.contains("<a href=\"/get_file.php?")) { // file
                downloadURL = PlugUtils.getStringBetween(contentAsString, "<a href=\"/get_file.php", "\"");
            } else if (contentAsString.contains("<img src=\"/get_file.php")) { //image
                downloadURL = PlugUtils.getStringBetween(contentAsString, "<img src=\"/get_file.php", "\" >");
            } else if (contentAsString.contains("video_player")) { //video stream
                isVideoStream = true;
                downloadURL = PlugUtils.getStringBetween(contentAsString, "playlist: '/get_file.php", "',");
            } else {
                checkProblems();
                throw new PluginImplementationException("Download link not found");
            }

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://www.putlocker.com/get_file.php" + downloadURL)
                    .toGetMethod();

            if (isVideoStream) {
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new PluginImplementationException();
                }
                contentAsString = getContentAsString();
                final String downloadURL2 = PlugUtils.getStringBetween(contentAsString, "url=\"", "\"");
                httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(downloadURL2)
                        .toGetMethod();
            }

            //logger.info(httpMethod.getURI().toString());

            //they sometimes wrap the name in quotes
            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);

            //here is the download link extraction
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
        if (contentAsString.contains("This file doesn't exist")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}