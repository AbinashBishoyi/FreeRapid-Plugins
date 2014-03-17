package cz.vity.freerapid.plugins.services.wupload_premium;

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
class WuploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(WuploadFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<span>Filename: </span> <strong>", "</strong>");
        PlugUtils.checkFileSize(httpFile, content, "class=\"size\">", "</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();
        login();
        setFileStreamContentTypes("\"application/octet-stream\"");
        final HttpMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download, account not premium?");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("This file has been deleted") || getContentAsString().contains("Page Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void login() throws Exception {
        synchronized (WuploadFileRunner.class) {
            WuploadServiceImpl service = (WuploadServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No Wupload account login information!");
                }
            }

            final HttpMethod method = getMethodBuilder()
                    .setAction("http://www.wupload.com/account/login")
                    .setParameter("email", pa.getUsername())
                    .setParameter("redirect", "/")
                    .setParameter("password", pa.getPassword())
                    .setParameter("rememberMe", "1")
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            if (getContentAsString().contains("No user found with such email") || getContentAsString().contains("Provided password does not match"))
                throw new BadLoginException("Invalid Wupload account login information!");
        }
    }

}