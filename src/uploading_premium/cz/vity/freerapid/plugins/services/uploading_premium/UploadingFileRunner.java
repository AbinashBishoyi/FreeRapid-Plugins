package cz.vity.freerapid.plugins.services.uploading_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class UploadingFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadingFileRunner.class.getName());
    private static String loginCookie = null;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".uploading.com", "setlang", "en", "/", 86400, false));
        addCookie(new Cookie(".uploading.com", "_lang", "en", "/", 86400, false));
        addCookie(new Cookie(".uploading.com", "lang", "1", "/", 86400, false));
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
        final Matcher match = PlugUtils.matcher("Filemanager</a></li>\\s*?<li>(.+?)</li>", getContentAsString());
        if (!match.find())
            throw new PluginImplementationException("File name not found");
        httpFile.setFileName(match.group(1).trim());
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<li class=\"size tip_container\">", "<div");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();
        login();
        final HttpMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private synchronized void login() throws Exception {
        if (loginCookie == null) {
            logger.info("Logging in...");
            try {
                UploadingServiceImpl service = (UploadingServiceImpl) getPluginService();
                PremiumAccount pa = service.getConfig();
                if (!pa.isSet()) {
                    synchronized (UploadingServiceImpl.class) {
                        pa = service.showConfigDialog();
                        if (pa == null || !pa.isSet()) {
                            throw new NotRecoverableDownloadException("No Uploading Premium account login information!");
                        }
                    }
                }
                final HttpMethod pm = getMethodBuilder()
                        .setAction("/general/login_form/")
                        .setParameter("email", pa.getUsername())
                        .setParameter("password", pa.getPassword())
                        .setParameter("remember", "1")
                        .setReferer(null)
                        .toPostMethod();
                if (!makeRedirectedRequest(pm)) {
                    throw new ServiceConnectionProblemException("Error posting login info");
                }
                if (getContentAsString().isEmpty()) {
                    throw new NotRecoverableDownloadException("Invalid Uploading Premium account login information!");
                }
                final Cookie cookie = getCookieByName("remembered_user");
                if (cookie != null) {
                    loginCookie = cookie.getValue();
                }
            } catch (final Exception e) {
                loginCookie = null;
                throw e;
            }
        } else {
            logger.info("Using cached login cookie");
            addCookie(new Cookie(".uploading.com", "remembered_user", loginCookie, "/", 86400, false));
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("The requested file is not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}