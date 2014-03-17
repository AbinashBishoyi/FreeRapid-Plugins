package cz.vity.freerapid.plugins.services.vimeo;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
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
        Matcher matcher;
        if (getContentAsString().contains("<meta property=\"og:title\" content=\"")) {
            name = PlugUtils.getStringBetween(getContentAsString(), "<meta property=\"og:title\" content=\"", "\"").trim();
        } else {
            matcher = getMatcherAgainstContent("<h1 itemprop=\"name\"[^<>]*?>(.+?)</h1>");
            if (!matcher.find()) {
                throw new PluginImplementationException("File name not found");
            }
            name = matcher.group(1).trim();
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
            fileURL = method.getURI().toString();
            if (fileURL.contains("/ondemand/") && getContentAsString().contains("Watch Trailer")) {
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromAHrefWhereATagContains("Watch Trailer")
                        .toGetMethod();
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            }
            VimeoVideo vimeoVideo = getSelectedVideo();
            logger.info("Config settings : " + config);
            logger.info("Downloading video : " + vimeoVideo);
            if (!tryDownloadAndSaveFile(getGetMethod(vimeoVideo.url))) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private VimeoVideo getSelectedVideo() throws ErrorDuringDownloadingException, IOException {
        if (getContentAsString().contains("data-config-url=\"")) {
            HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(PlugUtils.replaceEntities(PlugUtils.getStringBetween(getContentAsString(), "data-config-url=\"", "\"")))
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

        } else {
            throw new PluginImplementationException("Data config URL not found");
        }

        Matcher matcher = getMatcherAgainstContent("\"(?:qualities|h264|vp6)\":\\{(.+?)\\},\"(?:codecs|hls)\"");
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting video streams");
        }
        String qualitiesContent = matcher.group(1);
        matcher = PlugUtils.matcher("\"(.+?)\":\\{(.+?)}", qualitiesContent);

        final List<VimeoVideo> videoList = new LinkedList<VimeoVideo>();
        while (matcher.find()) {
            for (final VideoQuality videoQuality : VideoQuality.values()) {
                if (videoQuality.name().toLowerCase(Locale.ENGLISH).equals(matcher.group(1))) {
                    final JSON json = new JSON(matcher.group(2));
                    final VimeoVideo video = new VimeoVideo(videoQuality, json.getStringVar("url"));
                    videoList.add(video);
                }
            }
        }
        if (videoList.isEmpty()) {
            throw new PluginImplementationException("Quality list is empty");
        }
        return Collections.min(videoList);
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
        private final static int LOWER_QUALITY_PENALTY = 10;
        private final VideoQuality videoQuality;
        private final String url;
        private final int weight;

        public VimeoVideo(final VideoQuality videoQuality, final String url) {
            this.videoQuality = videoQuality;
            this.url = url;
            this.weight = calcWeight();
            logger.info("Found video: " + this);
        }

        private int calcWeight() {
            final VideoQuality configQuality = config.getVideoQuality();
            //prefer nearest better if the same quality doesn't exist
            return videoQuality.compareTo(configQuality) < 0
                    ? Math.abs(videoQuality.ordinal() - configQuality.ordinal()) + LOWER_QUALITY_PENALTY
                    : videoQuality.ordinal() - configQuality.ordinal();
        }

        public String getQualityLabel() {
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
                    ", url='" + url + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }

}