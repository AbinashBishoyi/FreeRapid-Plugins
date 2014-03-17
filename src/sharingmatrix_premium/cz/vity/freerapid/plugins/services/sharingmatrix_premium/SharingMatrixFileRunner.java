package cz.vity.freerapid.plugins.services.sharingmatrix_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class SharingMatrixFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SharingMatrixFileRunner.class.getName());
    private boolean badConfig = false;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".sharingmatrix.com", "lang", "en", "/", 86400, false));
        final GetMethod method = getGetMethod(fileURL);
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
        addCookie(new Cookie(".sharingmatrix.com", "lang", "en", "/", 86400, false));
        login();
        final GetMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            if (getContentAsString().contains("Regular Download")) {
                throw new BadLoginException("Problem logging in, account not premium?");
            }
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        try {
            PlugUtils.checkName(httpFile, getContentAsString(), "Filename:</span> <strong>", "</strong>");
            PlugUtils.checkFileSize(httpFile, getContentAsString(), "File size: <strong>", "</strong>");
        } catch (PluginImplementationException e) {
            //we don't want the plugin to break unnecessarily due to changes on the free download page
            LogUtils.processException(logger, e);
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File not found") || content.contains("File has been deleted")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void login() throws Exception {
        synchronized (SharingMatrixFileRunner.class) {
            SharingMatrixServiceImpl service = (SharingMatrixServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet() || badConfig) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No SharingMatrix Premium account login information!");
                }
                badConfig = false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction("http://sharingmatrix.com/ajax_scripts/login.php")
                    .setParameter("email", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("remember_me", "true")
                    .toGetMethod();
            httpMethod.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");

            if (!getContentAsString().equals("1"))
                throw new BadLoginException("Invalid SharingMatrix Premium account login information!");
        }
    }

}