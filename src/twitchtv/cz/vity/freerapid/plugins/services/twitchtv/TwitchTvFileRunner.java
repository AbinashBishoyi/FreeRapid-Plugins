package cz.vity.freerapid.plugins.services.twitchtv;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
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

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (isVideoUrl()) return;
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
        PlugUtils.checkName(httpFile, content, "<h2 id='broadcast_title'>", "</h2>");
        httpFile.setFileName(httpFile.getFileName() + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        if (isVideoUrl()) {
            processVideoUrl();
            return;
        }
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            isAtHomePage(method);
            checkProblems();
            checkNameAndSize(contentAsString);
            final int archiveId = PlugUtils.getNumberBetween(getContentAsString(), "\"archive_id\":", ",");
            HttpMethod httpMethod = getGetMethod(String.format("http://api.justin.tv/api/broadcast/by_archive/%d.xml", archiveId));
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final Matcher matcher = Pattern.compile("<video_file_url>(.+?)</video_file_url>.+?<title>(.+?)</title>", Pattern.DOTALL).matcher(getContentAsString());
            final List<URI> uriList = new LinkedList<URI>();
            final HashMap<String, Integer> titleMap = new HashMap<String, Integer>(); //to store unique title, value=counter
            while (matcher.find()) {
                //add title and counter to url's tail so we can extract it later.
                //http://media6.justin.tv/archives/2012-8-30/live_user_wries_1346350462.flv -> original videoUrl
                //http://media6.justin.tv/archives/2012-8-30/live_user_wries_1346350462.flv/česká kvalifikace na ESWC!! 2/4_2 -> title+"_"+counter added
                String title = matcher.group(2);
                Integer value;
                if ((value = titleMap.get(title)) == null) {
                    value = 1;
                    titleMap.put(title, value); //value=1 if key doesn't exist
                } else {
                    titleMap.put(title, ++value); //increase value if key exists
                }
                title = value == 1 ? URLEncoder.encode(title, "UTF-8") : URLEncoder.encode(title + "_" + value, "UTF-8");
                final String videoUrl = String.format("%s/%s", matcher.group(1), title);
                uriList.add(new URI(videoUrl));
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
        if (method.getURI().toString().matches("http://(www\\.)?(cs\\.)?twitch\\.tv/.+?/videos/?")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private boolean isVideoUrl() {
        return fileURL.matches("http://media\\d+\\.justin\\.tv/archives/.+?/.+?\\..{3}/.+");
    }

    private void processVideoUrl() throws Exception {
        //add title and counter to url's tail so we can extract it later.
        //http://media6.justin.tv/archives/2012-8-30/live_user_wries_1346350462.flv -> original videoUrl
        //http://media6.justin.tv/archives/2012-8-30/live_user_wries_1346350462.flv/česká kvalifikace na ESWC!! 2/4_2 -> title+"_"+counter added
        final Matcher matcher = PlugUtils.matcher("(http://media\\d+\\.justin\\.tv/archives/.+?/.+?\\..{3})/(.+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing video URL");
        }
        fileURL = matcher.group(1);
        final String extension = fileURL.substring(fileURL.lastIndexOf("."));
        httpFile.setFileName(URLDecoder.decode(matcher.group(2), "UTF-8") + extension);
        GetMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

}