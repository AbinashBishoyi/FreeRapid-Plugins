package cz.vity.freerapid.plugins.services.vimeo;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class VimeoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(VimeoFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        String fileExtension = PlugUtils.getStringBetween(getContentAsString(), "<div class=\"file_extension\">", "</div>").toLowerCase(Locale.ENGLISH);
        if (!fileExtension.startsWith(".")) fileExtension = "." + fileExtension;

        PlugUtils.checkName(httpFile, getContentAsString(), "<div class=\"title\">", "</div>");
        httpFile.setFileName(httpFile.getFileName() + fileExtension);

        PlugUtils.checkFileSize(httpFile, getContentAsString(), ">,\n<strong>", "</strong>");

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
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
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