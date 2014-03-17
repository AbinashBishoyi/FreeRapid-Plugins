package cz.vity.freerapid.plugins.services.crocko_premium;

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
class CrockoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CrockoFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".crocko.com", "language", "en", "/", null, false));
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
        final Matcher matcher = getMatcherAgainstContent("<span class=\"fz24\">\\s*Download:\\s*<strong>(.+?)</strong>\\s*</span>\\s*<span class=\"tip1\">\\s*<span class=\"inner\">(.+?)</span>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name/size not found");
        }
        httpFile.setFileName(matcher.group(1));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".crocko.com", "language", "en", "/", null, false));
        login();
        final HttpMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download, account not premium?");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("the page you're looking for") || content.contains("Requested file is deleted")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("The requested file is temporarily unavailable")) {
            throw new ServiceConnectionProblemException("The requested file is temporarily unavailable");
        }
    }

    private void login() throws Exception {
        synchronized (CrockoFileRunner.class) {
            CrockoServiceImpl service = (CrockoServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No Crocko account login information!");
                }
            }
            final HttpMethod method = getMethodBuilder()
                    .setAction("https://www.crocko.com/accounts/login")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("remember", "1")
                    .toPostMethod();
            method.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            if (getContentAsString().isEmpty()) {
                throw new BadLoginException("Invalid Crocko account login information!");
            }
        }
    }

}