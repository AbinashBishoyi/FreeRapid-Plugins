package cz.vity.freerapid.plugins.services.twitchtv;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import jlibs.core.net.URLUtil;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class TwitchTvFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TwitchTvFileRunner.class.getName());
    private final static String TITLE_PARAM = "title";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (isVideoUrl(fileURL)) {
            return;
        }
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
        String filename;
        try {
            filename = PlugUtils.getStringBetween(content, "\"title\":\"", "\"");
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("Filename not found");
        }
        httpFile.setFileName(filename.trim().replaceAll("\\s", " "));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        if (isVideoUrl(fileURL)) {
            processVideoUrl();
            return;
        }
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            isAtHomePage(method);
            checkProblems();
            checkNameAndSize(contentAsString);
            final int archiveId;
            try {
                archiveId = PlugUtils.getNumberBetween(getContentAsString(), "\"archive_id\":", ",");
            } catch (PluginImplementationException e) {
                throw new PluginImplementationException("Archive id not found");
            }
            final HttpMethod httpMethod = getGetMethod(String.format("http://api.justin.tv/api/broadcast/by_archive/%d.xml", archiveId));
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            checkProblems();
            //title element sometimes in front of video_file_url element, but sometimes behind video_file_url element
            final Matcher archiveMatcher = Pattern.compile("<archive>(.+?)</archive>", Pattern.DOTALL).matcher(getContentAsString());
            final Matcher titleMatcher = Pattern.compile("<title>(.+?)</title>").matcher(getContentAsString());
            final Matcher videoUrlMatcher = Pattern.compile("<video_file_url>(.+?)</video_file_url>").matcher(getContentAsString());
            final List<URI> uriList = new LinkedList<URI>();
            final HashMap<String, Integer> titleMap = new HashMap<String, Integer>(); //to store unique title, value=counter
            while (archiveMatcher.find()) {
                titleMatcher.region(archiveMatcher.start(1), archiveMatcher.end(1));
                if (!titleMatcher.find()) {
                    throw new PluginImplementationException("Title not found");
                }
                String title = titleMatcher.group(1);

                videoUrlMatcher.region(archiveMatcher.start(1), archiveMatcher.end(1));
                if (!videoUrlMatcher.find()) {
                    throw new PluginImplementationException("Video url not found");
                }
                String videoUrl = videoUrlMatcher.group(1);

                title = PlugUtils.unescapeHtml(title.replaceAll("\\s", " ")); // to avoid illegal chars in filename
                Integer counter = titleMap.get(title);
                if (counter == null) {
                    counter = 1;
                    titleMap.put(title, counter); //counter=1 if title doesn't exist
                } else {
                    titleMap.put(title, ++counter); //increase counter if title exists
                }
                if (counter != 1) {
                    title += "_" + counter;
                }

                //add title and counter to url's tail so we can extract it later.
                //http://media6.justin.tv/archives/2012-8-30/live_user_wries_1346350462.flv -> original videoUrl
                //http://media6.justin.tv/archives/2012-8-30/live_user_wries_1346350462.flv?title=česká kvalifikace na ESWC!! 2/4_2 -> title+"_"+counter added
                final String url = getMethodBuilder().setAction(videoUrl).setAndEncodeParameter(TITLE_PARAM, title).getEscapedURI();
                if (!isVideoUrl(url)) {
                    throw new PluginImplementationException("Unrecognized video url pattern"); //to prevent original link disappear when video url pattern unrecognized
                }
                uriList.add(new URI(url));
            }
            if (uriList.isEmpty()) {
                throw new PluginImplementationException("No video links found");
            }
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            httpFile.setState(DownloadState.COMPLETED);
            httpFile.getProperties().put("removeCompleted", true);
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

    private void isAtHomePage(final HttpMethod method) throws URLNotAvailableAnymoreException, URIException {
        if (method.getURI().toString().matches("http://(?:.+?\\.)?(?:twitch|justin)\\.tv/.+?/videos/?")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private boolean isVideoUrl(final String url) {
        return url.matches("http://.*?media\\d+\\.justin\\.tv/archives/.+?/.+?\\..{3}.*");
    }

    private void processVideoUrl() throws Exception {
        //http://media6.justin.tv/archives/2012-8-30/live_user_wries_1346350462.flv -> original videoUrl
        //http://media6.justin.tv/archives/2012-8-30/live_user_wries_1346350462.flv?title=česká kvalifikace na ESWC!! 2/4_2 -> title+"_"+counter added
        URL url = new URL(fileURL);
        String title = null;
        try {
            title = URLUtil.getQueryParams(fileURL, "UTF-8").get(TITLE_PARAM);
        } catch (Exception e) {
            //
        }
        fileURL = url.getProtocol() + "://" + url.getAuthority() + url.getPath();
        final String extension = fileURL.substring(fileURL.lastIndexOf("."));
        final String filename = (title == null ? fileURL.substring(fileURL.lastIndexOf("/") + 1) : URLDecoder.decode(title + extension, "UTF-8"));
        httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(filename, "_"));
        client.setReferer(httpFile.getFileUrl().getProtocol() + "://" + httpFile.getFileUrl().getAuthority());
        final GetMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

}