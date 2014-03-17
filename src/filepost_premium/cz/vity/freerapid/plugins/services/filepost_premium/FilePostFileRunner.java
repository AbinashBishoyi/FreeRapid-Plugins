package cz.vity.freerapid.plugins.services.filepost_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class FilePostFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilePostFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, getContentAsString(), "<title>FilePost.com: Download", "- fast &amp; secure!</title>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<span>Size:</span>", "</li>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            method = getMethodBuilder().setActionFromTextBetween("download_file('", "');").toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    public void login() throws Exception {
        FilePostServiceImpl service = (FilePostServiceImpl) getPluginService();
        PremiumAccount pa = service.getConfig();
        synchronized (FilePostFileRunner.class) {
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No FilePost Premium account login information!");
                }
            }
        }
        HttpMethod method = getGetMethod(getBaseURL());
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        final Cookie sid = getCookieByName("SID");
        final Cookie time = getCookieByName("time");
        if (sid == null || time == null) {
            throw new PluginImplementationException("Cookies not found");
        }
        method = getMethodBuilder()
                .setAction("/general/login_form/?SID=" + sid.getValue() + "&JsHttpRequest=" + time.getValue() + "-xml")
                .setParameter("email", pa.getUsername())
                .setParameter("password", pa.getPassword())
                .setParameter("remember", "on")
                .toPostMethod();
        method.setRequestHeader("Content-Type", "application/octet-stream; charset=UTF-8");
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException("Error posting login info");
        }
        if (!getContentAsString().contains("\"success\":true")) {
            throw new BadLoginException("Invalid FilePost Premium account login information!");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}