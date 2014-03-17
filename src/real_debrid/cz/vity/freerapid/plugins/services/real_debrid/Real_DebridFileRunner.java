package cz.vity.freerapid.plugins.services.real_debrid;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
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
 * @author birchie
 */
class Real_DebridFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Real_DebridFileRunner.class.getName());
    final String RD_BASE_URL = "https://real_debrid.com";
    final String RD_LOGIN = "https://real-debrid.com/ajax/login.php";
    final String RD_GENERATE = "https://real-debrid.com/ajax/unrestrict.php";

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        final GetMethod method = getGetMethod(RD_GENERATE);
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            final HttpMethod postMethod = getMethodBuilder()
                    .setAction(RD_GENERATE)
                    .setParameter("link", fileURL)
                    .setParameter("password", "")
                    .setParameter("remote", "0")
                    .setParameter("time", "" + System.currentTimeMillis())
                    .setBaseURL(RD_BASE_URL)
                    .setReferer(RD_BASE_URL)
                    .toGetMethod();
            postMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRequest(postMethod)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            final String content = getContentAsString();
            checkRequest(content);
            httpFile.setFileName(PlugUtils.getStringBetween(content, "file_name\":\"", "\","));
            final String dlLink = PlugUtils.getStringBetween(content, "main_link\":\"", "\",");
            final String genLinks = PlugUtils.getStringBetween(content, "generated_links\":[[", "]],");
            final Matcher match = PlugUtils.matcher("\"(.+?)\",\"(.+?)\",\"(.+?)\"", genLinks);
            while (match.find()) {
                if (match.group(3).equals(dlLink)) {
                    httpFile.setFileName(match.group(1));
                    break;
                }
            }
            final HttpMethod dlMethod = getGetMethod(dlLink.replace("\\", ""));
            if (!tryDownloadAndSaveFile(dlMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkRequest(final String content) throws Exception {
        if (content.contains("You are not a Premium user, you can only use a part"))
            throw new ErrorDuringDownloadingException("Free use for some services only during Happy Hours");
        else if (content.contains("Your file is unavailable"))
            throw new URLNotAvailableAnymoreException("File not found");
        else if (content.contains("Upgrade your account"))
            throw new NotRecoverableDownloadException("Only for premium users");
    }

    private void checkProblems() throws Exception {
        final String content = getContentAsString();
        if (content.contains("Your free daily limit has been reached")) {         // todo  ???
            throw new ErrorDuringDownloadingException("Your free daily limit has been reached");
        }
    }

    private void login() throws Exception {
        synchronized (Real_DebridFileRunner.class) {
            cz.vity.freerapid.plugins.services.real_debrid.Real_DebridServiceImpl service = (cz.vity.freerapid.plugins.services.real_debrid.Real_DebridServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No Real-Debrid account login information!");
                }
            }
            HttpMethod method = getMethodBuilder()
                    .setAction(RD_LOGIN)
                    .setParameter("user", pa.getUsername())
                    .setParameter("pass", pa.getPassword())
                    .setParameter("captcha_challenge", "")
                    .setParameter("captcha_answer", "")
                    .setParameter("time", "" + System.currentTimeMillis())
                    .setBaseURL(RD_BASE_URL)
                    .setReferer(RD_BASE_URL)
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            if (getContentAsString().contains("Your login informations are incorrect")) {
                throw new BadLoginException("Invalid Real-Debrid account login information!");
            }
            final String auth = PlugUtils.getStringBetween(getContentAsString(), "cookie\":\"auth=", ";");
            addCookie(new Cookie(".real-debrid.com", "auth", auth, "/", 86400, false));
        }
    }

}