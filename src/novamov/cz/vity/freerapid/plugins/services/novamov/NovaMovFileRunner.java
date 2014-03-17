package cz.vity.freerapid.plugins.services.novamov;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class NovaMovFileRunner extends XFileSharingRunner {
    private final static Logger logger = Logger.getLogger(NovaMovFileRunner.class.getName());
    private final static String SERVICE_BASE_URL = "http://www.novamov.com";

    @Override
    public void runCheck() throws Exception {
        if (fileURL.matches("http://(?:www\\.)?novaup\\.com/.+")) {
            httpFile.setNewURL(new URL(fileURL.replaceFirst("novaup\\.com", "novamov.com")));
        }
        super.runCheck();
    }

    @Override
    protected void checkFileSize() throws ErrorDuringDownloadingException {

    }

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new NovaMovFileNameHandler());
        return fileNameHandlers;
    }

    @Override
    public void run() throws Exception {
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkFileProblems();
            checkNameAndSize();

            final String user = "undefined";
            final String pass = "undefined";
            final String file = PlugUtils.getStringBetween(getContentAsString(), "flashvars.file=\"", "\";");
            final String key = PlugUtils.getStringBetween(getContentAsString(), "flashvars.filekey=\"", "\";");
            final String codes = PlugUtils.getStringBetween(getContentAsString(), "flashvars.cid=\"", "\";");
            final String player = SERVICE_BASE_URL + PlugUtils.getStringBetween(getContentAsString(), "swfobject.embedSWF(", ",").replace("\"", "");
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(player)
                    .setAction("http://www.novamov.com/api/player.api.php")
                    .setParameter("user", user)
                    .setParameter("codes", codes)
                    .setParameter("pass", pass)
                    .setParameter("file", file)
                    .setParameter("key", key)
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkDownloadProblems();
                throw new ServiceConnectionProblemException();
            }
            checkDownloadProblems();

            final String videoURL = "http" + PlugUtils.getStringBetween(getContentAsString().replaceFirst("url=", ""), "http", "&title");
            final Matcher extMatcher = PlugUtils.matcher("http://[^/]+/.+?\\.(.+?)&title", getContentAsString().replaceFirst("url=", ""));
            final String ext;
            if (extMatcher.find()) {
                ext = "." + extMatcher.group(1);
            } else {
                ext = ".flv";
            }
            httpFile.setFileName(httpFile.getFileName() + ext);

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(videoURL)
                    .setParameter("client", "FLASH")
                    .toGetMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkDownloadProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file no longer exists")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        super.checkFileProblems();
    }
}