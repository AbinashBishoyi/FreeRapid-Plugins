package cz.vity.freerapid.plugins.services.fileserve_premium;

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
class FileServeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileServeFileRunner.class.getName());

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
        runCheck();
        login();
        final HttpMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download, account not premium?");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<h1>", "<");
        final Matcher size = getMatcherAgainstContent("<strong>\\s*(\\d.+?)\\s*</strong>");
        if (!size.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size.group(1).replace(",", "")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("The file could not be found") || getContentAsString().contains("Page not found") || getContentAsString().contains("File not available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("/error.php")) {
            throw new ServiceConnectionProblemException("Temporary server issue");
        }
    }

    private void login() throws Exception {
        synchronized (FileServeFileRunner.class) {
            FileServeServiceImpl service = (FileServeServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No FileServe account login information!");
                }
            }

            final HttpMethod method = getMethodBuilder()
                    .setAction("http://www.fileserve.com/login.php")
                    .setReferer("http://www.fileserve.com/index.php")
                    .setParameter("loginUserName", pa.getUsername())
                    .setParameter("loginUserPassword", pa.getPassword())
                    .setParameter("autoLogin", "on")
                    .setParameter("loginFormSubmit", "Login")
                    .toPostMethod();
            makeRequest(method);//not redirected, might cause issues otherwise

            if (getContentAsString().contains("Username doesn't exist") || getContentAsString().contains("Wrong password") || getContentAsString().contains("should be larger than or equal to"))
                throw new BadLoginException("Invalid FileServe account login information!");
        }
    }

}