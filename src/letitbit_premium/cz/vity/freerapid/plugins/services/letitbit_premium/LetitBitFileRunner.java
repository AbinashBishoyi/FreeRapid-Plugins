package cz.vity.freerapid.plugins.services.letitbit_premium;

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
class LetitBitFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LetitBitFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".letitbit.net", "lang", "en", "/", 86400, false));
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
        addCookie(new Cookie(".letitbit.net", "lang", "en", "/", 86400, false));
        login();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            //name and size are not visible for premium users
            method = getMethodBuilder()
                    .setReferer(method.getURI().toString())
                    .setActionFromAHrefWhereATagContains("ownload")
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

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String name = PlugUtils.getStringBetween(getContentAsString(), "<span class=\"file-info-name\">", "</span>");
        httpFile.setFileName(PlugUtils.unescapeHtml(name).trim());
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<span class=\"file-info-size\">[", "]</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("The page is temporarily unavailable")) {
            throw new ServiceConnectionProblemException("The page is temporarily unavailable");
        }
        if (content.contains("The file is temporarily unavailable")) {
            throw new ServiceConnectionProblemException("The file is temporarily unavailable");
        }
        if (content.contains("file was not found")
                || content.contains("\u043D\u0430\u0439\u0434\u0435\u043D")
                || content.contains("<title>404</title>")
                || (content.contains("Request file ") && content.contains(" Deleted"))
                || content.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("You do not have enough premium points to download this file")) {
            throw new NotRecoverableDownloadException("You do not have enough premium points to download this file");
        }
    }

    private void login() throws Exception {
        synchronized (LetitBitFileRunner.class) {
            final LetitBitServiceImpl service = (LetitBitServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (pa == null || !pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No LetitBit Premium account login information!");
                }
            }
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer("http://letitbit.net/")
                    .setAction("/index.php")
                    .setParameter("act", "login")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");
            if (getContentAsString().contains("Authorization data is invalid")
                    || getContentAsString().contains("Login is indicated in wrong format"))
                throw new BadLoginException("Invalid LetitBit Premium account login information!");
        }
    }

}