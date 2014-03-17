package cz.vity.freerapid.plugins.services.alldebrid;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class AllDebridFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(cz.vity.freerapid.plugins.services.alldebrid.AllDebridFileRunner.class.getName());
    final String AD_BASE_URL = "http://www.alldebrid.com";
    final String AD_LOGIN = "http://www.alldebrid.com/register/";
    final String AD_GENERATE = "http://www.alldebrid.com/service/";

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        final GetMethod method = getGetMethod(AD_GENERATE);
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            final HttpMethod postMethod = getMethodBuilder()
                    .setActionFromFormWhereTagContains("Alldebrid", false)
                    .setParameter("link", fileURL)
                    .setParameter("nb", "0")
                    .setParameter("json", "true")
                    .setParameter("pw", "")
                    .setAction(AD_BASE_URL + "/service.php")
                    .setReferer(AD_GENERATE)
                    .toGetMethod();
            postMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRequest(postMethod)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            final String content = getContentAsString();
            checkRequest(content);
            httpFile.setFileName(PlugUtils.getStringBetween(content, "filename\":\"", "\","));
            final HttpMethod dlMethod = getGetMethod(PlugUtils.getStringBetween(content, "link\":\"", "\",").replace("\\", ""));
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
        if (content.matches("login"))
            throw new BadLoginException("You need to login");
        else if (content.contains("error\":\"Trial offer only"))
            throw new ErrorDuringDownloadingException("Host not available on trial offer");
        else if (content.contains("error\":\"This link isn't available on the host"))
            throw new URLNotAvailableAnymoreException("File not found");
        else if (content.contains("error\":\"This link isn't valid or not supported"))
            throw new NotRecoverableDownloadException("Link isn't valid or not supported");
        else if (!content.contains("error\":\"\""))
            throw new PluginImplementationException("Other error : " + PlugUtils.getStringBetween(content, "error\":\"", "\""));
    }

    private void checkProblems() throws Exception {
        final String content = getContentAsString();
        if (content.contains("Your free daily limit has been reached")) {         // todo  ???
            throw new ErrorDuringDownloadingException("Your free daily limit has been reached");
        }
    }

    private void login() throws Exception {
        synchronized (cz.vity.freerapid.plugins.services.alldebrid.AllDebridFileRunner.class) {
            cz.vity.freerapid.plugins.services.alldebrid.AllDebridServiceImpl service = (cz.vity.freerapid.plugins.services.alldebrid.AllDebridServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No AllDebrid account login information!");
                }
            }
            HttpMethod method = getGetMethod(AD_LOGIN);
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();//check problems
            method = getMethodBuilder()
                    .setActionFromFormByName("connectform", true)
                    .setParameter("login_login", pa.getUsername())
                    .setParameter("login_password", pa.getPassword())
                    .setBaseURL(AD_BASE_URL)
                    .setReferer(AD_LOGIN)
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            if (getContentAsString().contains("Your subscription is expired")) {
                throw new BadLoginException("Your AllDebrid subscription has expired!");
            }
            if (!getContentAsString().contains("your account will expire")) {
                throw new BadLoginException("Invalid AllDebrid account login information!");
            }
        }
    }

}