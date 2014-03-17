package cz.vity.freerapid.plugins.services.sockshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.URIUtil;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 * @author tong2shot
 */
class SockShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SockShareFileRunner.class.getName());

    private final static int REDIRECT_MAX_DEPTH = 4;
    private SettingsConfig config;

    private void setConfig() throws Exception {
        SockShareServiceImpl service = (SockShareServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private void checkUrl() {
        if (!fileURL.startsWith("http://www.")) {
            fileURL = fileURL.replaceFirst("http://", "http://www.");
        }
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkUrl();
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
        checkUrl();
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
                if (getContentAsString().contains("exceeded the daily download limit for your country")) {
                    throw new PluginImplementationException("The daily download limit for your country has been exceeded");
                }
            }

            final String downloadURL;
            boolean isVideoStream = false;
            final String getFileAHRef = "<a href=\"/get_file.php";
            setConfig();
            final VideoQuality configQuality = config.getVideoQuality();
            logger.info("Config settings : " + config);
            if (getContentAsString().contains("video_player")) { //video stream
                if ((configQuality == VideoQuality.High) && (getContentAsString().contains(getFileAHRef))) {  //large file (same as downloading the file)
                    downloadURL = PlugUtils.getStringBetween(getContentAsString(), getFileAHRef, "\"");
                } else { // small file (download the stream)
                    isVideoStream = true;
                    downloadURL = PlugUtils.getStringBetween(getContentAsString(), "playlist: '/get_file.php", "',");
                }
            } else if (getContentAsString().contains(getFileAHRef)) { // file
                downloadURL = PlugUtils.getStringBetween(getContentAsString(), getFileAHRef, "\"");
            } else if (getContentAsString().contains("<img src=\"/get_file.php")) { //image
                downloadURL = PlugUtils.getStringBetween(getContentAsString(), "<img src=\"/get_file.php", "\" >");
            } else {
                throw new PluginImplementationException("Download link not found");
            }

            String mobileDownloadURL = downloadURL.contains("original=1") ? downloadURL.replaceFirst("original=1", "mobile=1") : downloadURL + "&mobile=1";
            if (configQuality == VideoQuality.Mobile) {
                if (!doDownload(baseURL, mobileDownloadURL, isVideoStream)) {
                    logger.info("Attempt to download mobile version failed .." + "\n" + "Trying to download the stream or original version ..");
                    if (!doDownload(baseURL, downloadURL, isVideoStream)) { //mobile version is unstable, if fails then download the stream or original version
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                }
            } else if (!doDownload(baseURL, downloadURL, isVideoStream)) {
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
        if (contentAsString.contains("This file doesn't exist")
                || contentAsString.contains("404 Not Found")
                || contentAsString.contains("File Does Not Exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("Server is Overloaded")) {
            throw new YouHaveToWaitException("Server is overloaded", 60 * 2);
        }
    }

    private boolean doDownload(String baseURL, String downloadURL, boolean isVideoStream) throws Exception {
        HttpMethod method = getMethodBuilder()
                .setReferer(fileURL)
                .setBaseURL(baseURL)
                .setAction("/get_file.php" + downloadURL)
                .toGetMethod();
        if (isVideoStream) {
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final String downloadURL2 = PlugUtils.getStringBetween(getContentAsString(), "url=\"", "\"");
            if (downloadURL2.contains("expired_link.gif")) {
                return false;
            }
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadURL2)
                    .toGetMethod();
        }

        //they sometimes wrap the name in quotes
        setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
        try {
            if (setFileExtAndTryDownloadAndSaveFile(getMethodBuilder().setReferer(fileURL).setAction(method.getURI().toString()).toGetMethod())) {  //"cloning" method, to prevent method being aborted
                return true;
            }
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
            return (setFileExtAndTryDownloadAndSaveFile(method));
        }
        return false;
    }

    //to make sure we set the correct file extension
    private boolean setFileExtAndTryDownloadAndSaveFile(HttpMethod method) throws Exception {
        Header locationHeader;
        String action = method.getURI().toString();
        int redirCount = 1;
        do {
            if (redirCount++ >= REDIRECT_MAX_DEPTH) {
                throw new PluginImplementationException("Maximum redirect depth exceeded");
            }
            final HttpMethod method2 = getMethodBuilder().setReferer(fileURL).setAction(action).toGetMethod();
            processHttpMethod(method2);
            if (method2.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                throw new URLNotAvailableAnymoreException("File not found");
            }
            locationHeader = method2.getResponseHeader("Location");
            if (locationHeader != null) {
                action = locationHeader.getValue();
            }
            method2.abort();
            method2.releaseConnection();
        } while (locationHeader != null);

        String filename = httpFile.getFileName();
        String path = URIUtil.getPath(action);
        String filenameFromUrl = path.substring(path.lastIndexOf("/") + 1); //get filename from path

        String ext = null;
        try {
            ext = filenameFromUrl.substring(filenameFromUrl.lastIndexOf("."));
        } catch (Exception e) { // doesn't have ext
            //
        }
        httpFile.setFileName((filename.matches(".+?\\.[^\\.]{3}$")) && (ext != null) ? filename.replaceFirst("\\.[^\\.]{3}$", ext) : (ext != null ? filename + ext : filename));
        method = getMethodBuilder().setReferer(fileURL + "#").setAction(action).toGetMethod();
        return super.tryDownloadAndSaveFile(method);
    }

    private void processHttpMethod(HttpMethod method) throws IOException {
        if (client.getHTTPClient().getHostConfiguration().getProtocol() != null) {
            client.getHTTPClient().getHostConfiguration().setHost(method.getURI().getHost(), 80, client.getHTTPClient().getHostConfiguration().getProtocol());
        }
        client.getHTTPClient().executeMethod(method);
    }

}