package cz.vity.freerapid.plugins.services.fileim;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLEncoder;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FileIMFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileIMFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".fileim.com", "SiteLang", "en-us", "/", 86400, false));
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
        PlugUtils.checkName(httpFile, content, "id=\"FileName\" title=\"", "\">");
        final long filesize = PlugUtils.getFileSizeFromString(PlugUtils.getStringBetween(content, "id=\"FileSize\">", "</label>").replace("(", "").replace(")", ""));
        httpFile.setFileSize(filesize);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".fileim.com", "SiteLang", "en-us", "/", 86400, false));
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);

            final String fid = PlugUtils.getStringBetween(getContentAsString(), "download.fid=\"", "\"");
            final String f = PlugUtils.getStringBetween(getContentAsString(), "download.f=\"", "\"");
            final String downloaderParams = PlugUtils.unescapeHtml(PlugUtils.getStringBetween(getContentAsString(), "id=\"av\" value=\"", "\""));
            final String time = PlugUtils.getStringBetween(getContentAsString(), "'time' rel='", "'");
            final String tzOffset = String.valueOf(-(TimeZone.getDefault().getOffset((new Date()).getTime())) / 1000 / 60 / 60); // UTC+7 = -7

            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("/ajax/FixTime.ashx")
                    .setParameter("utcs", URLEncoder.encode(time, "UTF-8"))
                    .setParameter("tzone", tzOffset)
                    .toGetMethod();
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("/ajax/download/getTimer.ashx")
                    .toGetMethod();
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final String[] getTimer = getContentAsString().split("_");
            final int waitTime = Integer.parseInt(getTimer[0]);

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("/ajax/download/setTimer.ashx")
                    .setParameter("fid", fid)
                    .setParameter("f", f)
                    .toGetMethod();
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            downloadTask.sleep(waitTime);

            final String downloaderAction = "/libs/downloader.aspx?a=" + downloaderParams;
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloaderAction)
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final String downloaderContent = getContentAsString();

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("/ajax/getKey.ashx")
                    .toGetMethod();
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            httpMethod = getMethodBuilder(downloaderContent)
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("Free Download")
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
        if (contentAsString.contains("the file or folder does not exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("$('.d3').show();")) {
            throw new YouHaveToWaitException("Another download is started, 1 download at the same time limited", 5 * 60);
        }
        if (contentAsString.contains("Illegal access")) {
            throw new PluginImplementationException("Plugin is broken - Illegal access");
        }
    }

}