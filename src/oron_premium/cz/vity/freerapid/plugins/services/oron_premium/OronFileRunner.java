package cz.vity.freerapid.plugins.services.oron_premium;

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
class OronFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(OronFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".oron.com", "lang", "english", "/", 86400, false));
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
        addCookie(new Cookie(".oron.com", "lang", "english", "/", 86400, false));

        login();

        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            if (getContentAsString().contains("Regular Download")) {
                throw new BadLoginException("Problem logging in, account not premium?");
            }
            method = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setActionFromFormByName("F1", true).toPostMethod();
            if (makeRedirectedRequest(method)) {
                checkProblems();
                method = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download File").toGetMethod();
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("Filename: <[^<>]*>([^<]+)<");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(matcher.group(1));

        PlugUtils.checkFileSize(httpFile, getContentAsString(), "ize:", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<p class=\"err\">([^<>]+)<");
        if (matcher.find()) {
            if (matcher.group(1).contains("No such file")) {
                throw new URLNotAvailableAnymoreException("File not found");
            }
        }
        if (getContentAsString().contains("<b>File Not Found</b>")
                || getContentAsString().contains("<meta NAME=\"description\" CONTENT=\"ORON.com - File Not Found\">")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("Expired session")) {
            throw new ServiceConnectionProblemException("Expired session");
        }
    }

    private void login() throws Exception {
        synchronized (OronFileRunner.class) {
            final OronServiceImpl service = (OronServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No Oron Premium account login information!");
                }
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction("/login")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("op", "login")
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");

            if (getContentAsString().contains("Incorrect Login or Password"))
                throw new BadLoginException("Invalid Oron Premium account login information!");
        }
    }

}