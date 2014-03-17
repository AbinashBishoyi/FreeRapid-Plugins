package cz.vity.freerapid.plugins.services.putlocker;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.URIUtil;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class PutLockerFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PutLockerFileRunner.class.getName());

    private PutLockerSettingsConfig getConfig() throws Exception {
        PutLockerServiceImpl service = (PutLockerServiceImpl) getPluginService();
        return service.getConfig();
    }

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
        PlugUtils.checkName(httpFile, content, "<h1>", "<strong>");
        PlugUtils.checkFileSize(httpFile, content, "<strong>(", ")</strong></h1>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String baseURL = "http://" + method.getURI().getAuthority();
            checkProblems();
            checkNameAndSize(getContentAsString());
            /* skip waiting time
            if (getContentAsString().contains("countdownNum")) {
                final int waitTime = PlugUtils.getWaitTimeBetween(getContentAsString(), "var countdownNum = ", ";", TimeUnit.SECONDS);
                downloadTask.sleep(waitTime); skip countdown
            }
            */
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("confirm", true)
                    .setAction(fileURL)
                    .setParameter("confirm", "Continue as Free User")
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            //refresh the page, if quota exceeded
            if (getContentAsString().contains("exceeded the daily download limit for your country")) {
                downloadTask.sleep(3);
                httpMethod = getGetMethod(fileURL);
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            }
            final String downloadURL;
            boolean isVideoStream = false;
            if (getContentAsString().contains("video_player")) { //video stream
                PutLockerSettingsConfig config = getConfig();
                final VideoQuality configQuality = config.getVideoQuality();
                if ((configQuality == VideoQuality.High) && (getContentAsString().contains("<a href=\"/get_file.php?"))) {  //large file (same as downloading the file)
                    downloadURL = PlugUtils.getStringBetween(getContentAsString(), "<a href=\"/get_file.php", "\"");
                } else { // small file (download the stream)
                    isVideoStream = true;
                    downloadURL = PlugUtils.getStringBetween(getContentAsString(), "playlist: '/get_file.php", "',");
                }
            } else if (getContentAsString().contains("<a href=\"/get_file.php?")) { // file
                downloadURL = PlugUtils.getStringBetween(getContentAsString(), "<a href=\"/get_file.php", "\"");
            } else if (getContentAsString().contains("<img src=\"/get_file.php")) { //image
                downloadURL = PlugUtils.getStringBetween(getContentAsString(), "<img src=\"/get_file.php", "\" >");
            } else {
                throw new PluginImplementationException("Download link not found");
            }
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setBaseURL(baseURL)
                    .setAction("/get_file.php" + downloadURL)
                    .toGetMethod();
            if (isVideoStream) {
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                final String downloadURL2 = PlugUtils.getStringBetween(getContentAsString(), "url=\"", "\"");
                httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(downloadURL2)
                        .toGetMethod();
            }

            //they sometimes wrap the name in quotes
            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
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
        if (contentAsString.contains("This file doesn't exist") || contentAsString.contains("404 Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("Server is Overloaded")) {
            throw new YouHaveToWaitException("Server is overloaded", 60 * 2);
        }
    }

    @Override
    protected boolean tryDownloadAndSaveFile(HttpMethod method) throws Exception {
        try {
            if (super.tryDownloadAndSaveFile(getMethodBuilder().setReferer(fileURL).setAction(method.getURI().toString()).toGetMethod()))  //"cloning" method, to prevent method being aborted
                return true;
        } catch (org.apache.commons.httpclient.InvalidRedirectLocationException e) {
            //they use "'" char in redirect url, we have to replace it.
            client.makeRequest(method, false);
            final Header locationHeader = method.getResponseHeader("Location");
            if (locationHeader == null) {
                throw new PluginImplementationException("Invalid redirect");
            }
            method = getMethodBuilder()
                    .setReferer(fileURL)
                            //.setAction(locationHeader.getValue().replace("'", "%27"))
                    .setAction(new org.apache.commons.httpclient.URI(URIUtil.encodePathQuery(locationHeader.getValue(), "UTF-8"), true, client.getHTTPClient().getParams().getUriCharset()).toString().replace("'", "%27"))
                    .toGetMethod();
            return (super.tryDownloadAndSaveFile(method));
        }
        return false;
    }
}