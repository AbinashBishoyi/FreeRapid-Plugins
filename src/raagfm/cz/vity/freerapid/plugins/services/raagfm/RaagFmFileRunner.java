package cz.vity.freerapid.plugins.services.raagfm;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
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
 */
class RaagFmFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RaagFmFileRunner.class.getName());
    private final static String SEC_CODE = "sec1Code=robol111@123";
    private RaagFmSettingsConfig config;


    private void setConfig() throws Exception {
        RaagFmServiceImpl service = (RaagFmServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void run() throws Exception {
        super.run();
        setConfig();
        logger.info("Starting download in TASK " + fileURL);
        if (isAlbumURL()) {
            final GetMethod method = getGetMethod(fileURL);
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            isAtHomepage(method);
            checkProblems();
            processAlbum();
        } else if (fileURL.contains("player.raag.fm/player/")) {
            final String referer = "http://player.raag.fm/player/flash/main.swf?xmlPath=" + fileURL;
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(referer)
                    .setAction(String.format("%s&%s", fileURL, SEC_CODE))
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final String musicPath = PlugUtils.getStringBetween(getContentAsString(), "<musicpath><![CDATA[", "]]>");
            final String musicTitle = PlugUtils.getStringBetween(getContentAsString(), "<musictitle><![CDATA[", "]]>");
            final String artist = PlugUtils.getStringBetween(getContentAsString(), "<artist><![CDATA[", "]]>");
            final Matcher matcher = PlugUtils.matcher("http://.+?/[^/]+(\\.\\w+)\\??", musicPath);
            final String extension;
            if (!matcher.find()) {
                extension = ".mp3";
            } else {
                extension = matcher.group(1);
            }
            httpFile.setFileName(String.format("%s - %s%s", artist, musicTitle, extension));
            httpMethod = getMethodBuilder()
                    .setReferer(referer)
                    .setAction(musicPath)
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            throw new PluginImplementationException("Cannot recognize URL");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void processAlbum() throws URIException, URISyntaxException, PluginImplementationException {
        final String quality;
        final int configQuality = config.getQualitySetting();
        if ((getContentAsString().contains("value=flash-hd")) && (configQuality == 1)) {
            quality = "flash-hd";
        } else if (getContentAsString().contains("value=flash")) {
            quality = "flash";
        } else {
            throw new PluginImplementationException("Cannot recognize audio quality");
        }
        final String urlListRegex = "<input type=['\"]checkbox['\"] name=['\"]pick\\[\\]['\"] value=(\\d+)>";
        final Matcher urlListMatcher = getMatcherAgainstContent(urlListRegex);
        final List<URI> uriList = new LinkedList<URI>();
        while (urlListMatcher.find()) {
            uriList.add(new java.net.URI(new org.apache.commons.httpclient.URI(String.format("http://player.raag.fm/player/flash/%s.asx?pick[]=%s", quality, urlListMatcher.group(1)), false, "UTF-8").toString()));
        }
        if (uriList.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        httpFile.getProperties().put("removeCompleted", true);
    }

    private void isAtHomepage(HttpMethod method) throws Exception {
        final String pageURL = method.getURI().toString();
        if (pageURL.matches("http://(?:music\\.raag\\.fm|music.*?\\.pz10\\.com)/?")) {
            throw new URLNotAvailableAnymoreException("File/Album not found");
        }
    }

    private boolean isAlbumURL() {
        return fileURL.matches("http://(?:music\\.raag\\.fm|music.*?\\.pz10\\.com)/.+");
    }

}