package cz.vity.freerapid.plugins.services.bandcamp;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u3
 */
class BandCampFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BandCampFileRunner.class.getName());

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
        String artist;
        String album;
        String filename;
        try {
            artist = PlugUtils.getStringBetween(content, "artist : \"", "\"");
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("Artist name not found");
        }
        try {
            album = PlugUtils.getStringBetween(content, "album_title : \"", "\"");
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("Album name not found");
        }
        if (isAlbum(fileURL)) {
            filename = artist + " - " + album;
        } else if (isTrack(fileURL)) {
            String title;
            try {
                title = PlugUtils.getStringBetween(content, "\"title\":\"", "\"");
            } catch (PluginImplementationException e) {
                throw new PluginImplementationException("Track title not found");
            }
            filename = artist + " - " + album + " - " + title + ".mp3";
        } else {
            throw new PluginImplementationException("Unknown URL pattern");
        }
        logger.info("File name : " + filename);
        httpFile.setFileName(filename);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            if (isAlbum(fileURL)) {
                parseAlbum(getContentAsString());
            } else if (isTrack(fileURL)) {
                Matcher trackInfoMatcher = PlugUtils.matcher("trackinfo\\s*:\\s*\\[\\{(.+?)\\}\\]", getContentAsString());
                if (!trackInfoMatcher.find()) {
                    throw new PluginImplementationException("Track info not found");
                }
                //it's possible, a track contains more than one audio quality.
                //not implemented yet, couldn't find sample.
                Matcher fileMatcher = PlugUtils.matcher("\"file\":\\{.+?\"(http:[^\"]+?)\"", trackInfoMatcher.group(1));
                if (!fileMatcher.find()) {
                    if (trackInfoMatcher.group(1).contains("\"file\":null")) {
                        throw new URLNotAvailableAnymoreException("File not found");
                    }
                    throw new PluginImplementationException("Track file not found");
                }
                final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(fileMatcher.group(1)).toHttpMethod();

                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                throw new PluginImplementationException("Unknown URL pattern");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Sorry, that something isn't here")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private boolean isTrack(String fileUrl) {
        return fileUrl.contains("/track/");
    }

    private boolean isAlbum(String fileUrl) {
        return fileUrl.contains("/album/");
    }

    private void parseAlbum(String content) throws Exception {
        List<URI> uriList = new LinkedList<URI>();
        Matcher trackInfoMatcher = PlugUtils.matcher("trackinfo\\s*:\\s*\\[\\{(.+?)\\}\\]", content);
        if (!trackInfoMatcher.find()) {
            throw new PluginImplementationException("Track info not found");
        }
        String baseUrl = getBaseURL();
        Matcher trackUrlMatcher = PlugUtils.matcher("\"title_link\":\"(.+?)\"", trackInfoMatcher.group(1));
        while (trackUrlMatcher.find()) {
            try {
                uriList.add(new URI(baseUrl + trackUrlMatcher.group(1)));
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        if (uriList.isEmpty()) {
            throw new PluginImplementationException("No tracks found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        logger.info(uriList.size() + " tracks added");
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
    }

}
