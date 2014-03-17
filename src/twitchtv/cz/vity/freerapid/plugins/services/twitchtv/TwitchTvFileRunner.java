package cz.vity.freerapid.plugins.services.twitchtv;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class TwitchTvFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TwitchTvFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            isAtHomePage(getMethod);
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h2 id='broadcast_title'>", "</h2>");
        httpFile.setFileName(httpFile.getFileName() + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            isAtHomePage(method);
            checkProblems();
            checkNameAndSize(contentAsString);
            final String swfURL = PlugUtils.getStringBetween(getContentAsString(), "swfobject.embedSWF(\"", "\"");
            final int archiveId = PlugUtils.getNumberBetween(getContentAsString(), "\"archive_id\":", ",");
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(swfURL)
                    .setAction(String.format("http://api.justin.tv/api/broadcast/by_archive/%d.xml", archiveId))
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final Pattern pattern = Pattern.compile(String.format("<id>%d</id>.+?<video_file_url>(.+?)</video_file_url>", archiveId), Pattern.DOTALL);
            final Matcher matcher = pattern.matcher(getContentAsString());
            if (!matcher.find()) {
                throw new PluginImplementationException("Video URL not found");
            }
            final String videoURL = matcher.group(1);
            httpMethod = getMethodBuilder()
                    .setReferer(swfURL)
                    .setAction(videoURL)
                    .toGetMethod();
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
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void isAtHomePage(final HttpMethod method) throws URLNotAvailableAnymoreException, URIException {
        if (method.getURI().toString().matches("http://(www\\.)?(cs\\.)?twitch\\.tv/.+?/videos/?")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }


    }

}