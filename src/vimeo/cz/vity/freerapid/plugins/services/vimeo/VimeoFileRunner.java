package cz.vity.freerapid.plugins.services.vimeo;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 * @author tong2shot
 */
class VimeoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(VimeoFileRunner.class.getName());
    private VimeoSettingsConfig config;

    private void setConfig() throws Exception {
        VimeoServiceImpl service = (VimeoServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".vimeo.com", "language", "en", "/", 86400, false));
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (isPasswordProtected()) return;
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String name;
        if (getContentAsString().contains("<meta property=\"og:title\" content=\"")) {
            name = PlugUtils.getStringBetween(getContentAsString(), "<meta property=\"og:title\" content=\"", "\"");
        } else {
            name = PlugUtils.getStringBetween(getContentAsString(), "<h1 itemprop=\"name\">", "</h1>");
        }
        httpFile.setFileName(PlugUtils.unescapeHtml(name) + ".mp4");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Page not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".vimeo.com", "language", "en", "/", 86400, false));
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            stepPassword();
            checkProblems();
            checkNameAndSize();
            setConfig();
            if (!tryDownloadAndSaveFile(getPlayMethod())) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private HttpMethod getPlayMethod() throws ErrorDuringDownloadingException {
        final String strQualities;
        if (getContentAsString().contains("\"qualities\":[")) {
            strQualities = PlugUtils.getStringBetween(getContentAsString(), "\"qualities\":[", "]").replace("\"", "");
        } else if (getContentAsString().contains("\"h264\":[")) {
            strQualities = PlugUtils.getStringBetween(getContentAsString(), "\"h264\":[", "]").replace("\"", "");
        } else {
            throw new PluginImplementationException("Error getting qualities token");
        }
        logger.info("Available qualities : " + strQualities);
        //Example : hd,sd,mobile
        final String[] qualityTokens = strQualities.split(",");
        final List<VimeoVideo> videoList = new LinkedList<VimeoVideo>();
        for (final VideoQuality videoQuality : VideoQuality.values()) {
            for (final String qualityToken : qualityTokens) {
                if (videoQuality.name().toLowerCase(Locale.ENGLISH).equals(qualityToken.trim())) {
                    final VimeoVideo video = new VimeoVideo(videoQuality);
                    videoList.add(video);
                    break;
                }
            }
        }
        if (videoList.isEmpty()) {
            throw new PluginImplementationException("Quality list is empty");
        }
        final String qualityToken = Collections.min(videoList).getQualityToken();

        final Matcher matcher = getMatcherAgainstContent("\\{config:\\{([^\r\n]+\\s*[^\r\n]+)");
        if (!matcher.find()) {
            throw new PluginImplementationException("Player config not found");
        }
        final JSON json = new JSON(matcher.group(1));
        return getGetMethod(String.format("http://%s/play_redirect?clip_id=%s&sig=%s&time=%s&quality=%s&codecs=%s&type=%s&embed_location=%s",
                json.getStringVar("player_url"),
                json.getNumVar("id"),
                json.getStringVar("signature"),
                json.getNumVar("timestamp"),
                qualityToken,
                "H264,VP8,VP6",
                "moogaloop_local",
                ""
        ));
    }

    private boolean isPasswordProtected() {
        return getContentAsString().contains("please provide the correct password");
    }

    private void stepPassword() throws Exception {
        while (isPasswordProtected()) {
            final String xsrft = PlugUtils.getStringBetween(getContentAsString(), "xsrft: '", "'");
            final String password = getDialogSupport().askForPassword("Vimeo");
            if (password == null) {
                throw new NotRecoverableDownloadException("This file is secured with a password");
            }
            final HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereActionContains("password", true)
                    .setParameter("password", password)
                    .setParameter("token", xsrft)
                    .toPostMethod();
            addCookie(new Cookie(".vimeo.com", "xsrft", xsrft, "/", 86400, false));
            makeRedirectedRequest(method); //http code : 418, if the entered password wrong
        }
    }

    private static class JSON {
        private final String content;

        public JSON(final String content) {
            this.content = content;
        }

        public String getStringVar(final String name) throws ErrorDuringDownloadingException {
            final Matcher matcher = PlugUtils.matcher("\"" + Pattern.quote(name) + "\"\\s*?:\\s*?\"(.*?)\"", content);
            if (!matcher.find()) {
                throw new PluginImplementationException("Parameter '" + name + "' not found");
            }
            return matcher.group(1);
        }

        public String getNumVar(final String name) throws ErrorDuringDownloadingException {
            final Matcher matcher = PlugUtils.matcher("\"" + Pattern.quote(name) + "\"\\s*?:\\s*?(\\d+)", content);
            if (!matcher.find()) {
                throw new PluginImplementationException("Parameter '" + name + "' not found");
            }
            return matcher.group(1);
        }
    }

    private class VimeoVideo implements Comparable<VimeoVideo> {
        private final static int NEAREST_LOWER_PENALTY = 10;
        private final VideoQuality videoQuality;
        private int weight;

        public VimeoVideo(final VideoQuality videoQuality) {
            this.videoQuality = videoQuality;
            calcWeight();
            logger.info("Found video: " + this);
        }

        private void calcWeight() {
            final VideoQuality configQuality = config.getVideoQuality();
            //prefer nearest better if the same quality doesn't exist
            weight = videoQuality.compareTo(configQuality) < 0
                    ? Math.abs(videoQuality.ordinal() - configQuality.ordinal()) + NEAREST_LOWER_PENALTY
                    : videoQuality.ordinal() - configQuality.ordinal();
        }

        public String getQualityToken() {
            logger.info("Downloading video: " + this);
            return videoQuality.name().toLowerCase(Locale.ENGLISH);
        }

        @Override
        public int compareTo(final VimeoVideo that) {
            return Integer.valueOf(this.weight).compareTo(that.weight);
        }

        @Override
        public String toString() {
            return "VimeoVideo{" +
                    "videoQuality=" + videoQuality +
                    ", weight=" + weight +
                    '}';
        }
    }

}