package cz.vity.freerapid.plugins.services.sendspace_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class SendSpaceFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SendSpaceFileRunner.class.getName());

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

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("start download")
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
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
        if (contentAsString.contains("404 Page Not Found")
                || contentAsString.contains("Sorry, the file you requested is not available")
                || contentAsString.contains("The page you are looking for is  not available")) {
            throw new URLNotAvailableAnymoreException("File was not found");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        final Matcher matcher = getMatcherAgainstContent("<h2 class=\"bgray\"><(?:b|strong)>(.+?)</");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(matcher.group(1));
        PlugUtils.checkFileSize(httpFile, contentAsString, "Size:</b>", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void login() throws Exception {
        synchronized (SendSpaceFileRunner.class) {
            SendSpaceServiceImpl service = (SendSpaceServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No SendSpace account login information!");
                }
            }
            final HttpMethod method = getMethodBuilder()
                    .setAction("/login.html")
                    .setParameter("action", "login")
                    .setParameter("submit", "login")
                    .setParameter("target", "/")
                    .setParameter("action_type", "login")
                    .setParameter("remember", "1")
                    .setParameter("username", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("remember", "on")
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            if (getContentAsString().contains("Login failed")) {
                throw new BadLoginException("Invalid SendSpace account login information!");
            }
        }
    }

}