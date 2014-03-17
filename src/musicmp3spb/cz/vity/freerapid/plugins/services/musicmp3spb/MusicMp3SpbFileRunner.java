package cz.vity.freerapid.plugins.services.musicmp3spb;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class MusicMp3SpbFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MusicMp3SpbFileRunner.class.getName());
    final String BASE_URL = "http://musicmp3spb.org";

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems(getMethod);
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems(getMethod);
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("musicmp3spb.org/(.+?)/", fileURL);
        if (!match.find())
            throw new InvalidURLOrServiceProblemException("Invalid Url");
        if (match.group(1).matches("album")) {
            httpFile.setFileName("Album: " + PlugUtils.getStringBetween(content, "<title>", " mp3</title>"));
        } else if (match.group(1).matches("allsongs")) {
            httpFile.setFileName("All Songs: " + PlugUtils.getStringBetween(content, "<h1>mp3 ", "</h1>"));
        } else if (match.group(1).matches("artist")) {
            httpFile.setFileName("Artist: " + PlugUtils.getStringBetween(content, "<h1>", " mp3</h1>"));
        } else if (match.group(1).matches("covers")) {
            final Matcher cover = PlugUtils.matcher("<h2>[^\\s]+? (.+?)</h2>", content);
            if (cover.find())
                if (cover.find())
                    httpFile.setFileName("Cover__" + cover.group(1).trim() + ".jpg");
                else throw new PluginImplementationException("cover name error2");
            else throw new PluginImplementationException("cover name error1");
        } else if (match.group(1).matches("download")) {
        } else if (match.group(1).matches("song")) {
            httpFile.setFileName("Song: " + PlugUtils.getStringBetween(content, "<title>", " mp3."));
            //httpFile.setFileSize(PlugUtils.getFileSizeFromString(PlugUtils.getStringBetween(content, "ер</b></td><td align=\"right\"><b>", "</b>") + "B"));
        } else
            throw new InvalidURLOrServiceProblemException("Unsupported Url");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems(method);//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            final Matcher match = PlugUtils.matcher("musicmp3spb.org/(.+?)/", fileURL);
            if (!match.find())
                throw new InvalidURLOrServiceProblemException("Invalid Url");
            if (match.group(1).matches("album") || match.group(1).matches("allsongs")) {
                buildList("<a href=\"(/song/.+?)\">", contentAsString);
            } else if (match.group(1).matches("artist")) {
                buildList("<a href=\"(/(album|allsongs)/.+?)\"", contentAsString);
            } else if (match.group(1).matches("covers")) {
                final Matcher cover = PlugUtils.matcher("<a href=\"(/images/.+?)\"", contentAsString);
                if (cover.find())
                    downloadFile(BASE_URL + cover.group(1));
                else
                    throw new PluginImplementationException("Cover image not found");
            } else if (match.group(1).matches("download")) {
                if (fileURL.contains("/play/"))
                    downloadFile(fileURL);
                else
                    getRedirectUrl(fileURL);
            } else if (match.group(1).matches("song")) {
                final Matcher song = PlugUtils.matcher(";<a href=\"(.+?)\"><img src=\"/img/down", contentAsString);
                if (song.find())
                    getRedirectUrl(BASE_URL + song.group(1));
                else
                    throw new PluginImplementationException("Song download not found");
            }
        } else {
            checkProblems(method);
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems(HttpMethod method) throws Exception {
        final String currentUri = method.getURI().getURI();
        if (currentUri.contains("/timeerror"))
            throw new ErrorDuringDownloadingException("Link expired. only available for 36 hours");
        if (currentUri.contains("/404.php"))
            throw new URLNotAvailableAnymoreException("File not found");

        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private void buildList(final String match, final String content) throws Exception {
        List<URI> list = new LinkedList<URI>();
        final Matcher m = PlugUtils.matcher(match, content);
        while (m.find()) {
            list.add(new URI(BASE_URL + m.group(1).trim()));
        }
        if (list.isEmpty()) throw new PluginImplementationException("No links found");
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.setFileName("Link(s) Extracted !");
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
    }

    private void getRedirectUrl(final String url) throws Exception {
        final GetMethod method = getGetMethod(url);
        if (makeRedirectedRequest(method)) {
            httpFile.setNewURL(new URL(method.getURI().getURI()));
            httpFile.setFileState(FileState.NOT_CHECKED);
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems(method);
            throw new ServiceConnectionProblemException();
        }
    }

    private void downloadFile(final String url) throws Exception {
        HttpMethod method = getGetMethod(url);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems(method);//if downloading failed
            throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
        }
    }

}