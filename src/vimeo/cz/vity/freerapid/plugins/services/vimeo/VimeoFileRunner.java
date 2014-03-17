package cz.vity.freerapid.plugins.services.vimeo;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

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
        httpFile.setFileName(httpFile.getFileName() + ".flv");
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
                json.getNumVar("hd").equals("1") ? "hd" : "sd",
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

}