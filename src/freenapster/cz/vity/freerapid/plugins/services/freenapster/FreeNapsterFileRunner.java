package cz.vity.freerapid.plugins.services.freenapster;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class FreeNapsterFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(FreeNapsterFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
            final String fragment = new URI(fileURL).getFragment();
            if (fragment != null && fragment.matches("\\d+")) {
                handleTrack(fragment);
            } else {
                handleAlbum();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Page Not Found") || getContentAsString().contains("Search Result Page")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void handleTrack(final String trackId) throws Exception {
        checkTrackName(trackId);
        setFileStreamContentTypes(new String[0], new String[]{"application/json;charset=UTF-8"});
        HttpMethod method = getGetMethod("http://music.nc.napster.com/player/mediadelivery/getStreamingURL.json?playerSessionId=&trackId=" + trackId + "02");
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        final String streamingUrl = PlugUtils.getStringBetween(getContentAsString(), "\"streamingURL\":\"", "\"");
        method = getGetMethod(streamingUrl);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        final String type = PlugUtils.getStringBetween(getContentAsString(), "<type>", "</type>");
        final String uid = PlugUtils.getStringBetween(getContentAsString(), "<uid>", "</uid>");
        final String server = PlugUtils.getStringBetween(getContentAsString(), "<server>", "</server>");
        final RtmpSession rtmpSession = new RtmpSession("rtmp://" + server, getPlayName(type, uid));
        tryDownloadAndSaveFile(rtmpSession);
    }

    private String getPlayName(final String type, final String uid) throws ErrorDuringDownloadingException {
        if ("AKAMAI".equals(type)) {
            return "flash/" + uid;
        } else if ("FMS".equals(type) && uid.length() >= 4) {
            return "flv:storage/" + uid.substring(0, 2) + "/" + uid.substring(2, 4) + "/" + uid;
        } else {
            throw new PluginImplementationException("Unknown stream type");
        }
    }

    private void checkTrackName(final String trackId) throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent(
                "<a type=\"track\" href=\"#" + trackId + "\"[^<>]*?>\\s*(.+?)\\s*</a>\\s*"
                        + "(?:<span class=\"explicit\">\\[EXPLICIT\\]</span>\\s*)?"
                        + "<span class=\"albumName\">\\s*On:\\s*<(?:span|a)[^<>]*?>\\s*(.+?)\\s*</(?:span|a)>\\s*</span>\\s*"
                        + "<span class=\"artistName\">\\s*By:\\s*<(?:span|a)[^<>]*?>\\s*(.+?)\\s*</(?:span|a)>\\s*</span>");
        if (!matcher.find()) {
            throw new PluginImplementationException("Track name not found");
        }
        httpFile.setFileName(PlugUtils.unescapeHtml(matcher.group(3)) + " - "
                + PlugUtils.unescapeHtml(matcher.group(1)) + ".flv");
    }

    private void handleAlbum() throws Exception {
        PlugUtils.checkName(httpFile, getContentAsString(), "<title>", "on Napster</title>");
        final String albumUrl = getAlbumUrl();
        final Matcher matcher = getMatcherAgainstContent("<a type=\"track\" href=\"(#\\d+)\"");
        final Set<URI> links = new LinkedHashSet<URI>();
        while (matcher.find()) {
            try {
                links.add(new URI(albumUrl + matcher.group(1)));
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        if (links.isEmpty()) {
            throw new PluginImplementationException("No tracks found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, new ArrayList<URI>(links));
        httpFile.getProperties().put("removeCompleted", true);
    }

    private String getAlbumUrl() throws ErrorDuringDownloadingException {
        final boolean playlist = fileURL.contains("/playlist.htm");
        final Matcher matcher = PlugUtils.matcher(playlist ? "/playlist.htm\\?id=(\\d+)" : "/album/(?:[^/]*?/)?(\\d+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing URL");
        }
        return (playlist ? "http://free.napster.com/playlist.htm?id=" : "http://free.napster.com/album/")
                + matcher.group(1);
    }

}