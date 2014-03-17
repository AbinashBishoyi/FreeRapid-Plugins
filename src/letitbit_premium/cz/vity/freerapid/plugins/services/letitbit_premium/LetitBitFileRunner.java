package cz.vity.freerapid.plugins.services.letitbit_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class LetitBitFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LetitBitFileRunner.class.getName());
    private boolean badConfig = false;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".letitbit.net", "lang", "en", "/", 86400, false));
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            PlugUtils.checkName(httpFile, getContentAsString(), "File::</span>", "</h1>");
            PlugUtils.checkFileSize(httpFile, getContentAsString(), "Size of file::</span>", "</h1>");
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".letitbit.net", "lang", "EN", "/", 86400, false));

        login();

        HttpMethod httpMethod = getMethodBuilder()
                .setReferer("http://premium.letitbit.net/index.php")
                .setAction("http://premium.letitbit.net/ajax.php?action=download")
                .setParameter("link", fileURL)
                .toPostMethod();
        httpMethod.setRequestHeader("X-Requested-With", "XMLHttpRequest");
        if (!makeRedirectedRequest(httpMethod))
            throw new ServiceConnectionProblemException();
        checkProblems();

        final Matcher matcher = getMatcherAgainstContent("Download file (.+?) \\((.+?)\\)");
        if (!matcher.find()) throw new PluginImplementationException("File name/size not found");
        httpFile.setFileName(matcher.group(1));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

        httpMethod = getMethodBuilder()
                .setReferer("http://premium.letitbit.net/index.php")
                .setActionFromAHrefWhereATagContains("Download file")
                .toGetMethod();
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("The page is temporarily unavailable")) {
            throw new ServiceConnectionProblemException("The page is temporarily unavailable");
        }
        if (content.contains("file was not found")
                || content.contains("\u043D\u0430\u0439\u0434\u0435\u043D")
                || content.contains("<title>404</title>")
                || (content.contains("Request file ") && content.contains(" Deleted"))) {
            throw new URLNotAvailableAnymoreException("The requested file was not found");
        }
        if (content.contains("You do not have enough premium points to download this file")) {
            throw new NotRecoverableDownloadException("You do not have enough premium points to download this file");
        }
    }

    private void login() throws Exception {
        synchronized (LetitBitFileRunner.class) {
            LetitBitServiceImpl service = (LetitBitServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet() || badConfig) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No LetitBit Premium account login information!");
                }
                badConfig = false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer("http://premium.letitbit.net/login.php")
                    .setAction("http://premium.letitbit.net/uajax.php?action=login")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("permanent", "true")
                    .toPostMethod();
            httpMethod.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");

            if (getContentAsString().equals("ENTERERROR"))
                throw new BadLoginException("You can not enter a premium account from this computer!");
            if (!getContentAsString().equals("OK"))
                throw new BadLoginException("Invalid LetitBit Premium account login information!");
        }
    }

}