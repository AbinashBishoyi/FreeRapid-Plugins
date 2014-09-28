package cz.vity.freerapid.plugins.services.turbobit_premium;

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
class TurboBitFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TurboBitFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".turbobit.net", "user_lang", "en", "/", 86400, false));
        fileURL = checkFileURL(fileURL);
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String checkFileURL(final String fileURL) throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("http://(?:www\\.)?turbobit\\.net/(?:download/free/)?([a-z0-9]+)(?:\\.html?)?", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing download link");
        }
        return "http://turbobit.net/" + matcher.group(1) + ".html";
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<span class.*?>(.+?)</span>\\s*\\((.+?)\\)");
        if (matcher.find()) {
            httpFile.setFileName(matcher.group(1));
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        } else {
            PlugUtils.checkName(httpFile, getContentAsString(), "file-title\">", "<");
            PlugUtils.checkFileSize(httpFile, getContentAsString(), "file-size\">", "</");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".turbobit.net", "user_lang", "en", "/", 86400, false));
        fileURL = checkFileURL(fileURL);
        login();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            method = getMethodBuilder().setActionFromAHrefWhereATagContains("<b>Download</b>").toGetMethod();
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
        if (getContentAsString().contains("Probably it was deleted")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("<u>Turbo Access</u> denied")) {
            throw new BadLoginException("Turbo Access denied");
        }
    }

    private void login() throws Exception {
        synchronized (TurboBitFileRunner.class) {
            TurboBitServiceImpl service = (TurboBitServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No TurboBit account login information!");
                }
            }
            final HttpMethod method = getMethodBuilder()
                    .setAction("http://turbobit.net/user/login")
                    .setParameter("user[login]", pa.getUsername())
                    .setParameter("user[pass]", pa.getPassword())
                    .setParameter("user[memory]", "on")
                    .setParameter("user[submit]", "Login")
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            if (getContentAsString().contains("<div class='error'>")) {
                throw new BadLoginException("Invalid TurboBit account login information!");
            }
        }
    }

}