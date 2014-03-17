package cz.vity.freerapid.plugins.services.rayfile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Tommy Yang
 */
class RayfileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RayfileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkFileUrl();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkSizeAndName();
            checkProblems();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        checkFileUrl();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkSizeAndName();
            fileURL = method.getURI().toString();
            final String fileVkey = getVKey();
            final String fileDLPage = fileURL + fileVkey + "/";
            method = getGetMethod(fileDLPage);

            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            final String downloadLink = PlugUtils.getStringBetween(getContentAsString(), "downloads_url = ['", "'");
            logger.info(String.format("Download link: %s", downloadLink));

            method = getMethodBuilder().setReferer(fileDLPage).setAction(downloadLink).toGetMethod();
            setCookie();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkSizeAndName() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "file_name = \"", "\"");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "formatsize = \"", "\"");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("您寻找的资源已经失效") || getContentAsString().contains("404 Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        } else if (getContentAsString().contains("因为文件太大或者该文件被盗链太严重")) {
            throw new ErrorDuringDownloadingException("File too large.");
        }
    }

    private String getFileId() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("/files/([^/]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        return matcher.group(1);
    }

    private String getVKey() throws PluginImplementationException {
        Matcher matcher = getMatcherAgainstContent("vkey = \"(.+?)\"");
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing vkey");
        }
        return matcher.group(1);
    }

    private void setCookie() throws PluginImplementationException {
        Matcher matcher = getMatcherAgainstContent("setCookie\\('(.+?)', '(.+?)', (.+?), '(.+?)'");
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing cookie content!");
        }
        final String name = matcher.group(1);
        final String value = matcher.group(2);
        final String path = matcher.group(4);
        final String domain = ".rayfile.com";
        this.addCookie(new Cookie(domain, name, value, path, 86400, false));
    }

    private void checkFileUrl() throws PluginImplementationException, UnsupportedEncodingException {
        Matcher matcher = PlugUtils.matcher("/files/([^/]+)/", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file url!");
        }
        fileURL = String.format("http://www.rayfile.com/zh-cn/files/%s/", matcher.group(1));
    }
}