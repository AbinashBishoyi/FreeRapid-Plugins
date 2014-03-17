package cz.vity.freerapid.plugins.services.veehd;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class VeehdFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(VeehdFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".veehd.com", "nsfw", "1", "/", 86400, false));
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
        PlugUtils.checkName(httpFile, content, "<h2 style=\"\">", "|");
        final String type = PlugUtils.getStringBetween(content, "type:", "<br").trim();
        final String ext;
        if (type.contains("flash")) {
            ext = ".flv";
        } else {
            ext = ".avi";
        }
        httpFile.setFileName(httpFile.getFileName() + ext);
        PlugUtils.checkFileSize(httpFile, content, "size:", "<br");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(".veehd.com", "nsfw", "1", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            final String ts = PlugUtils.getStringBetween(getContentAsString(), "var ts = \"", "\"");
            final String sgn = PlugUtils.getStringBetween(getContentAsString(), "var sgn = \"", "\"");
            final String vpi = "/vpi" + PlugUtils.getStringBetween(getContentAsString(), "/vpi", "\"");

            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://veehd.com/xhrp")
                    .setParameter("v", "c2")
                    .setParameter("p", "1")
                    .setParameter("ts", ts)
                    .setParameter("sgn", sgn)
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://veehd.com" + vpi)
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final String videoUrl;
            if (getContentAsString().contains("param name=\"src\" value=\""))
                videoUrl = PlugUtils.getStringBetween(getContentAsString(), "param name=\"src\" value=\"", "\"");
            else if (getContentAsString().contains("embed type=\"video/divx\" src=\""))
                videoUrl = PlugUtils.getStringBetween(getContentAsString(), "embed type=\"video/divx\" src=\"", "\"");
            else
                videoUrl = URLDecoder.decode(PlugUtils.getStringBetween(getContentAsString(), "url\":\"", "\""), "UTF-8");
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(videoUrl)
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
        if (contentAsString.contains("This is a private video") ||
                contentAsString.contains("This video has been removed due")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}