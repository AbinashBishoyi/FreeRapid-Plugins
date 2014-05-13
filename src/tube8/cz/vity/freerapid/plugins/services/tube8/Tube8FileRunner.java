package cz.vity.freerapid.plugins.services.tube8;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author TommyTom
 */
class Tube8FileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Tube8FileRunner.class.getName());

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
        final Matcher match = PlugUtils.matcher("videotitle\\s*?=\\s*?['\"]([^'\"]+?)['\"]", content);
        if (!match.find())
            throw new PluginImplementationException("File name not found");
        //remove old extension if it exists
        String name = match.group(1).trim();
        final int pos = name.lastIndexOf('.');
        if (pos != -1) name = name.substring(0, pos);
        httpFile.setFileName(name + ".flv");
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

            Matcher match = PlugUtils.matcher("videoHash\\s*?=\\s*?['\"]([^\"']+?)['\"]", contentAsString);
            if (!match.find())
                throw new PluginImplementationException("Token 'hash' not found");
            final String hash = match.group(1).trim();
            match = PlugUtils.matcher("videoidVar\\s*?=\\s*?['\"]([^'\"]+?)['\"]", contentAsString);
            if (!match.find())
                throw new PluginImplementationException("Token 'videoId' not found");
            final String videoId = match.group(1).trim();
            final MethodBuilder ajaxMethodBuilder = getMethodBuilder()
                    .setAjax()
                    .setAction("http://www.tube8.com/ajax/getVideoDownloadURL.php")
                    .setParameter("hash", hash)
                    .setParameter("video", videoId);
            final HttpMethod ajaxMethod = ajaxMethodBuilder.toGetMethod();

            logger.info("Making ajax request..");
            if (makeRequest(ajaxMethod)) {
                contentAsString = getContentAsString();
                logger.info("Ajax response : " + contentAsString);
                //there are 2 options for video to download : standard and mobile
                //we choose standard
                final String videoURL = PlugUtils.getStringBetween(contentAsString, "\"standard_url\":\"", "\",").replace("\\", "");
                final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(videoURL).toGetMethod();
                setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
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
        final String contentAsString = getContentAsString();
        if (contentAsString.contains(">Newest Videos<")) {
            throw new URLNotAvailableAnymoreException("File not found");
        } else if (contentAsString.contains("This video is deleted") ||
                contentAsString.contains("video-removed-div")) {
            throw new URLNotAvailableAnymoreException("This video is deleted");
        }
    }

}