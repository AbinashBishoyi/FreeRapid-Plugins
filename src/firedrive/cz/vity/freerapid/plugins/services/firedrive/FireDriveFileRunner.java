package cz.vity.freerapid.plugins.services.firedrive;

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
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u3
 */
class FireDriveFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FireDriveFileRunner.class.getName());
    private final static int REDIRECT_MAX_DEPTH = 4;

    private SettingsConfig config;

    private void setConfig() throws Exception {
        FireDriveServiceImpl service = (FireDriveServiceImpl) getPluginService();
        config = service.getConfig();
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

    private void checkUrl() {
        if (!fileURL.startsWith("http://www.")) {
            fileURL = fileURL.replaceFirst("http://", "http://www.");
        }
        if (fileURL.contains("putlocker.com")) {
            fileURL = fileURL.replaceFirst("putlocker\\.com", "firedrive.com");
        }
    }

    @Override
    protected String getBaseURL() {
        return "http://www.firedrive.com";
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<b>Name:</b>", "<br>");
        PlugUtils.checkFileSize(httpFile, content, "<b>Size:</b>", "<br>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkUrl();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("confirm_form", true)
                    .setAction(fileURL)
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            setConfig();
            VideoQuality configQuality = config.getVideoQuality();
            logger.info("Config settings : " + config);
            Matcher hiMatcher = getMatcherAgainstContent("[\"'](http://[^\"']+?\\?key=[^\"']+?)[\"']");
            Matcher lowMatcher = getMatcherAgainstContent("[\"'](http://[^\"']+?\\?stream=[^\"']+?)[\"']");
            String downloadURL;
            //low quality stream is not guaranteed to be exist.
            //high quality on the other hand, always exists.
            if (lowMatcher.find()) {
                if ((configQuality == VideoQuality.High) && (hiMatcher.find())) {
                    downloadURL = hiMatcher.group(1);
                } else {
                    downloadURL = lowMatcher.group(1);
                }
            } else if (hiMatcher.find()) {
                downloadURL = hiMatcher.group(1);
            } else {
                throw new PluginImplementationException("Download link not found");
            }

            if (configQuality == VideoQuality.Mobile) {
                String mobileDownloadURL = downloadURL.replaceFirst("\\?(key|stream)=", "?mobile=");
                if (!tryDownloadAndSaveFile(getGetMethod(mobileDownloadURL))) {
                    logger.info("Attempt to download mobile version failed ..\nTrying to download the stream or original version ..");
                    if (!tryDownloadAndSaveFile(getGetMethod(downloadURL))) { //mobile version is unstable, if fails then download low or high quality
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                }
            } else if (!tryDownloadAndSaveFile(getGetMethod(downloadURL))) {
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
        if (contentAsString.contains("File Does Not Exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("Server is Overloaded")) {
            throw new YouHaveToWaitException("Server is overloaded", 60 * 2);
        }
    }

    @Override
    protected boolean tryDownloadAndSaveFile(HttpMethod method) throws Exception {
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
        //file ext pattern too broad ?
        httpFile.setFileName((filename.matches(".+?\\.[^\\.]{3}$")) && (ext != null) ? filename.replaceFirst("\\.[^\\.]{3}$", ext) : (ext != null ? filename + ext : filename));
        method = getMethodBuilder().setReferer(fileURL).setAction(action).toGetMethod();
        return super.tryDownloadAndSaveFile(method);
    }

    private void processHttpMethod(HttpMethod method) throws IOException {
        if (client.getHTTPClient().getHostConfiguration().getProtocol() != null) {
            client.getHTTPClient().getHostConfiguration().setHost(method.getURI().getHost(), 80, client.getHTTPClient().getHostConfiguration().getProtocol());
        }
        client.getHTTPClient().executeMethod(method);
    }
}
