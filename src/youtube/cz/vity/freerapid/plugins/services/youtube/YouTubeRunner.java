package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda, JPEXS, ntoskrnl
 * @since 0.82
 */
class YouTubeFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(YouTubeFileRunner.class.getName());
    private static final String SERVICE_WEB = "http://www.youtube.com";
    private static final URI SERVICE_URI = URI.create(SERVICE_WEB);
    private YouTubeSettingsConfig config;
    private int fmt = 0;
    private String fileExtension = ".flv";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            setConfig();
            checkName();

            if (isUserPage()) {
                parseUserPage();
                return;
            }

            checkFmtParameter();
            checkName();

            String fmt_url_map = PlugUtils.getStringBetween(getContentAsString(), "&fmt_url_map=", "&");
            fmt_url_map = URLDecoder.decode(fmt_url_map, "UTF-8");
            Matcher matcher = PlugUtils.matcher("," + fmt + "\\|(http[^\\|]+)(,[0-9]+\\||$)", "," + fmt_url_map);

            if (matcher.find()) {
                client.getHTTPClient().getParams().setBooleanParameter("dontUseHeaderFilename", true);
                getMethod = getGetMethod(matcher.group(1));
                if (!tryDownloadAndSaveFile(getMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                throw new PluginImplementationException("Cannot find specified video format (" + fmt + ")");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        /* Causes false positives
        final Matcher matcher = getMatcherAgainstContent("<div\\s+?class=\"yt-alert-content\">\\s*([^<>]+?)\\s*</div>");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(matcher.group(1));
        }
        */
    }

    private void checkName() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<meta name=\"title\" content=\"", "\"");
        String fileName = PlugUtils.unescapeHtml(PlugUtils.unescapeHtml(httpFile.getFileName()));
        if (!isUserPage()) {
            fileName += fileExtension;
        }
        httpFile.setFileName(fileName);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean isUserPage() {
        return fileURL.contains("/user/");
    }

    private void setConfig() throws Exception {
        YouTubeServiceImpl service = (YouTubeServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private void checkFmtParameter() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("fmt=(\\d+)", fileURL.toLowerCase(Locale.ENGLISH));

        if (matcher.find()) {
            final String fmtCode = matcher.group(1);

            if (fmtCode.length() <= 2) {
                fmt = Integer.parseInt(fmtCode);
                setFileExtension(fmt);
            }
        } else {
            processConfig();
        }
    }

    private void processConfig() throws ErrorDuringDownloadingException {
        String fmt_map = PlugUtils.getStringBetween(getContentAsString(), "&fmt_map=", "&");
        try {
            fmt_map = URLDecoder.decode(fmt_map, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LogUtils.processException(logger, e);
        }
        String formats[] = fmt_map.split(",");
        int quality = config.getQualitySetting();
        if (quality == 4) quality = formats.length - 1; //maximum available
        if (quality >= formats.length) quality = formats.length - 1;
        String selectedFormat = formats[formats.length - 1 - quality];
        fmt = Integer.parseInt(selectedFormat.substring(0, selectedFormat.indexOf("/")));
        setFileExtension(fmt);
    }

    private void setFileExtension(int fmtCode) {
        switch (fmtCode) {
            case 13:
            case 17:
                fileExtension = ".3gp";
                break;
            case 18:
            case 22:
                fileExtension = ".mp4";
                break;
        }
    }

    private void parseUserPage() throws Exception {
        Matcher matcher = PlugUtils.matcher(".+/([^\\?&#]+)", fileURL);
        if (!matcher.find()) throw new PluginImplementationException("Error parsing file URL");
        final String user = matcher.group(1);

        final List<URI> uriList = new LinkedList<URI>();

        logger.info("Trying method 1");
        HttpMethod method = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("http://www.youtube.com/profile_ajax?action_ajax=1&user=" + user + "&new=1&box_method=load_playlist_videos_multi&box_name=user_playlist_navigator&playlistName=all")
                .setParameter("session_token", "")
                .setParameter("messages", "[{\"type\":\"box_method\",\"request\":{\"name\":\"user_playlist_navigator\",\"x_position\":1,\"y_position\":-2,\"palette\":\"default\",\"method\":\"load_playlist_videos_multi\",\"params\":{\"playlist_name\":\"all\",\"view\":\"grid\",\"playlist_sort\":\"date\"}}}]")
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        matcher = PlugUtils.matcher("<a href=\"([^\"]+?)\" class=\"video-thumb", getContentAsString().replace("\\\"", "\"").replace("\\/", "/"));
        while (matcher.find()) {
            try {
                uriList.add(SERVICE_URI.resolve(new URI(matcher.group(1))));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }

        if (uriList.isEmpty()) {
            logger.info("Trying method 2");
            int lastSize = -1;
            int page = 1;
            while (uriList.size() != lastSize) {
                lastSize = uriList.size();
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction("http://www.youtube.com/profile_ajax?action_ajax=1&user=" + user + "&new=1&box_method=load_playlist&box_name=user_playlist_navigator&playlistName=uploads&sort=date")
                        .setParameter("session_token", "")
                        .setParameter("messages", "[{\"type\":\"box_method\",\"request\":{\"name\":\"user_playlist_navigator\",\"x_position\":1,\"y_position\":-2,\"palette\":\"default\",\"method\":\"load_playlist_page\",\"params\":{\"playlist_name\":\"uploads\",\"encrypted_playlist_id\":\"uploads\",\"query\":\"\",\"encrypted_shmoovie_id\":\"uploads\",\"page_num\":" + page++ + ",\"view\":\"grid\",\"playlist_sort\":\"date\"}}}]")
                        .toPostMethod();
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
                matcher = PlugUtils.matcher("<a href=\"([^\"]+?)\" class=\"video-thumb", getContentAsString().replace("\\\"", "\"").replace("\\/", "/"));
                while (matcher.find()) {
                    try {
                        uriList.add(SERVICE_URI.resolve(new URI(matcher.group(1))));
                    } catch (URISyntaxException e) {
                        LogUtils.processException(logger, e);
                    }
                }
            }
        }

        // YouTube returns the videos in descending date order, which is a bit illogical.
        // If the user wants them that way, don't reverse.
        if (!config.isReversePlaylistOrder()) {
            Collections.reverse(uriList);
        }

        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        logger.info(uriList.size() + " videos added");
        if (!uriList.isEmpty()) {
            httpFile.getProperties().put("removeCompleted", true);
        }
    }

}