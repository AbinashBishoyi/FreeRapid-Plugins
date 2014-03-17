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
    private final static String[] qualityTokenMap = {"mobile", "sd", "hd"};
    private VimeoSettingsConfig config;

    private void setConfig() throws Exception {
        VimeoServiceImpl service = (VimeoServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private String getQualityToken(final int quality) {
        return qualityTokenMap[quality];
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
        if (getContentAsString().contains("<meta property=\"og:title\" content=\"")) {
            PlugUtils.checkName(httpFile, getContentAsString(), "<meta property=\"og:title\" content=\"", "\"");
        } else {
            PlugUtils.checkName(httpFile, getContentAsString(), "<h1 itemprop=\"name\">", "</h1>");
        }
        httpFile.setFileName(httpFile.getFileName() + ".mp4");
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
            while (isPasswordProtected()) {
                final String xsrft = PlugUtils.getStringBetween(getContentAsString(), "xsrft: '", "'");
                final String password = getDialogSupport().askForPassword("Vimeo");
                if (password == null) {
                    throw new NotRecoverableDownloadException("This file is secured with a password");
                }
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromFormWhereActionContains("password", true)
                        .setParameter("password", password)
                        .setParameter("token", xsrft)
                        .toPostMethod();
                addCookie(new Cookie(".vimeo.com", "xsrft", xsrft, "/", 86400, false));
                makeRedirectedRequest(method); //http code : 418, if the entered password wrong
            }
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
        } else throw new PluginImplementationException("Error getting qualities token");
        logger.info("Available qualities : " + strQualities);
        //Example : hd,sd,mobile
        final String[] qualityTokens = strQualities.split(",");
        final List<VimeoVideo> vvList = new LinkedList<VimeoVideo>();
        for (String qualityToken : qualityTokens) {
            for (int j = 0; j < qualityTokenMap.length; j++) {
                if (qualityToken.trim().equals(getQualityToken(j))) {
                    final VimeoVideo vv = new VimeoVideo(j, qualityToken.trim());
                    vvList.add(vv);
                    logger.info(vv.toString());
                    break;
                }
            }
        }
        if (vvList.isEmpty()) throw new PluginImplementationException("Quality list is empty");
        final String qualityToken = Collections.min(vvList).qualityToken;

        final Matcher matcher = getMatcherAgainstContent("\\{config:\\{([^\r\n]+)");
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
        private final int quality;
        private final String qualityToken;
        private int weight;

        private VimeoVideo(int quality, String qualityToken) {
            this.quality = quality;
            this.qualityToken = qualityToken;
            calcWeight();
        }

        private void calcWeight() {
            final int configQuality = config.getQualitySetting();
            weight = ((quality - configQuality) < 0) ? (Math.abs(quality - configQuality) + NEAREST_LOWER_PENALTY) : (quality - configQuality); //prefer nearest better if the same quality doesn't exist
        }

        @Override
        public int compareTo(VimeoVideo that) {
            return Integer.valueOf(this.weight).compareTo(that.weight);
        }

        @Override
        public String toString() {
            return "weight         = " + weight + "\n" +
                    "quality        = " + quality + "\n" +
                    "qualityToken   = " + qualityToken + "\n";
        }
    }

}