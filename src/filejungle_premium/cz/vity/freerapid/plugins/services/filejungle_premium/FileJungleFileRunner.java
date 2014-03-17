package cz.vity.freerapid.plugins.services.filejungle_premium;

import cz.vity.freerapid.plugins.exceptions.BadLoginException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class FileJungleFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileJungleFileRunner.class.getName());

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
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "<div id=\"file_name\">", "<span class=\"filename_normal\">");
        PlugUtils.checkFileSize(httpFile, content, "<span class=\"filename_normal\">(", ")</span></div>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();
        login();
        HttpMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            checkNameAndSize();
            method = getMethodBuilder().setActionFromFormByName("premiumForm", true).toPostMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File not available")) {
            throw new URLNotAvailableAnymoreException("File not available");
        }
    }

    private void login() throws Exception {
        synchronized (FileJungleFileRunner.class) {
            FileJungleServiceImpl service = (FileJungleServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No FileJungle account login information!");
                }
            }

            final HttpMethod method = getMethodBuilder()
                    .setAction("/login.php")
                    .setReferer("/index.php")
                    .setParameter("autoLogin", "on")
                    .setParameter("loginUserName", pa.getUsername())
                    .setParameter("loginUserPassword", pa.getPassword())
                    .setParameter("loginFormSubmit", "")
                    .toPostMethod();
            makeRequest(method);//not redirected, might cause issues otherwise

            if (getContentAsString().contains("Username doesn't exist")
                    || getContentAsString().contains("Wrong password")
                    || getContentAsString().contains("should be larger than or equal to")) {
                throw new BadLoginException("Invalid FileJungle account login information!");
            }
        }
    }

}