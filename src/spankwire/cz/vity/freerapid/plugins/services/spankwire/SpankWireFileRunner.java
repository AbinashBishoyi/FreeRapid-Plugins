package cz.vity.freerapid.plugins.services.spankwire;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author User
 */
class SpankWireFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SpankWireFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems(getMethod);
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkFileProblems(getMethod);
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h1>", "</h1>");
        httpFile.setFileName(httpFile.getFileName() + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkFileProblems(method);
            checkProblems();//check problems
            checkNameAndSize(getContentAsString());//extract file name and size from the page

            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(getSelectedVideoUrl(getContentAsString()))
                    .toHttpMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkFileProblems(method);
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getSelectedVideoUrl(String content) throws UnsupportedEncodingException, PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("flashvars\\.quality_(\\d+)p\\s*?=\\s*?\"([^\"]+)\"", content);
        Map<Integer, String> videoMap = new HashMap<Integer, String>();
        while (matcher.find()) {
            int quality = Integer.parseInt(matcher.group(1));
            String videoUrl = matcher.group(2).trim();
            if (!videoUrl.isEmpty()) {
                videoMap.put(quality, URLDecoder.decode(videoUrl, "UTF-8"));
            }
        }
        if (videoMap.isEmpty()) {
            throw new PluginImplementationException("No videos available");
        }
        return videoMap.get(Collections.max(videoMap.keySet()));
    }

    private void checkFileProblems(HttpMethod method) throws Exception {
        if (method.getURI().toString().matches("http://(?:www\\.)spankwire\\.com/?")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Error Page Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("This article is temporarily unavailable.")) {
            throw new YouHaveToWaitException("This article is temporarily unavailable. Please try again in a few minutes.", 5);
        }
    }

}