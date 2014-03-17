package cz.vity.freerapid.plugins.services.tvuoluolcom;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class TvuolUolComFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TvuolUolComFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<span class=\"fn\">", "</span>");
        httpFile.setFileName(httpFile.getFileName() + ".mp4");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);

            final Matcher mediaIdMatcher = getMatcherAgainstContent("\"mediaId\"\\s*:(.+?),");
            if (!mediaIdMatcher.find()) {
                throw new PluginImplementationException("Media ID not found");
            }
            final String mediaId = mediaIdMatcher.group(1).trim().replace("+", "").replace("\"", "");

            final Matcher playerMatcher = getMatcherAgainstContent("\"player\"\\s*:(.+?),");
            if (!playerMatcher.find()) {
                throw new PluginImplementationException("Embedded player not found");
            }
            final String player = playerMatcher.group(1).trim().replace("\"", "");

            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://mais.uol.com.br/apiuol/player/media.js")
                    .setParameter("mediaId", mediaId)
                    .setParameter("p", "tvuol")
                    .setParameter("action", "showPlayer")
                    .setParameter("types", "V")
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            final Matcher videoHDMatcher = getMatcherAgainstContent("(\"http://videohd1.+?\")");
            final Matcher videoSDMatcher = getMatcherAgainstContent("(\"http://video\\d+.+?\")");
            String videoURL;
            if (videoHDMatcher.find()) { // HD as default
                videoURL = videoHDMatcher.group(1).trim().replace("\"", "");
            } else if (videoSDMatcher.find()) {
                videoURL = videoSDMatcher.group(1).trim().replace("\"", "");
            } else {
                throw new PluginImplementationException("Video URL not found");
            }

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(videoURL)
                    .setParameter("ver", "1")
                    .setParameter("r", player + "?mediaId=" + mediaId + "&tv=1&p=tvuol")
                    .toGetMethod();
            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("O vídeo não foi encontrado")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}