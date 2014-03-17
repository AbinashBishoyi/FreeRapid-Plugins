package cz.vity.freerapid.plugins.services.videoslasher;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u3
 */
class VideoSlasherFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(VideoSlasherFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            check404Page(getMethod);
            checkProblems();
            checkNameAndSize();
        } else {
            check404Page(getMethod);
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("<h1 style=\"margin:20px 20px 10px 20px\">(.+)\\((.+?)\\)</h1>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name/size not found");
        }
        String filename = matcher.group(1).trim();
        long filesize = PlugUtils.getFileSizeFromString(matcher.group(2).trim());
        logger.info("File name : " + filename);
        logger.info("File size : " + filesize);
        httpFile.setFileName(filename);
        httpFile.setFileSize(filesize);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            check404Page(method);
            checkProblems();
            checkNameAndSize();
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("confirm", true)
                    .setAction(fileURL)
                    .setParameter("confirm", "Close Ad and Watch as Free User")
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            String playlistUrl;
            try {
                playlistUrl = PlugUtils.getStringBetween(getContentAsString(), "playlist: '", "'");
            } catch (PluginImplementationException e) {
                throw new PluginImplementationException("Playlist URL not found");
            }
            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(playlistUrl).toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            String videoUrl;
            try {
                videoUrl = PlugUtils.getStringBetween(getContentAsString(), "<title>Video</title><media:content url=\"", "\"");
            } catch (PluginImplementationException e) {
                throw new PluginImplementationException("Video URL not found");
            }
            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(videoUrl).toGetMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            check404Page(method);
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

    private void check404Page(HttpMethod method) throws URIException, URLNotAvailableAnymoreException {
        if (method.getURI().toString().matches("http://(www\\.)?videoslasher\\.com/404")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}
