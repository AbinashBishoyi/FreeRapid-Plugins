package cz.vity.freerapid.plugins.services.bitshare_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class BitShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BitShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".bitshare.com", "language_selection", "EN", "/", 86400, false));
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
        PlugUtils.checkName(httpFile, content, "<h1>Downloading ", " - ");
        PlugUtils.checkFileSize(httpFile, content, " - ", "</h1>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        final int i = fileURL.lastIndexOf('/');
        if (i > 0) {
            final int i2 = fileURL.toLowerCase(Locale.ENGLISH).lastIndexOf(".html");
            if (i2 > i) {
                httpFile.setFileName(fileURL.substring(i + 1, i2));
            }
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".bitshare.com", "language_selection", "EN", "/", 86400, false));
        login();
        setFileStreamContentTypes("text/plain");
        HttpMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            setFileStreamContentTypes(new String[0], new String[]{"application/json"});
            final String url = PlugUtils.getStringBetween(getContentAsString(), "url: \"", "\"");
            final String ajaxdl = PlugUtils.getStringBetween(getContentAsString(), "var ajaxdl = \"", "\";");
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(url)
                    .setParameter("request", "generateID")
                    .setParameter("ajaxid", ajaxdl)
                    .toPostMethod();
            method.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(url)
                    .setParameter("request", "getDownloadURL")
                    .setParameter("ajaxid", ajaxdl)
                    .toPostMethod();
            method.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final String[] data = getContentAsString().split("#");
            if (data.length < 2 || !data[1].startsWith("http")) {
                throw new PluginImplementationException("Error parsing server response");
            }
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(data[1])
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("requested file was not found in our database")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void login() throws Exception {
        synchronized (BitShareFileRunner.class) {
            BitShareServiceImpl service = (BitShareServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No BitShare account login information!");
                }
            }
            final HttpMethod method = getMethodBuilder()
                    .setAction("http://bitshare.com/login.html")
                    .setParameter("user", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("rememberlogin", "")
                    .setParameter("submit", "Login")
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            if (getContentAsString().contains(">Login</h1>")) {
                throw new BadLoginException("Invalid BitShare account login information!");
            }
        }
    }

}