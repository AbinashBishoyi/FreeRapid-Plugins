package cz.vity.freerapid.plugins.services.simply_debrid;

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
class Simply_DebridFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Simply_DebridFileRunner.class.getName());
    final String SD_BASE_URL = "http://simply-debrid.com/";
    final String SD_LOGIN = "http://simply-debrid.com/login";
    final String SD_GENERATE = "http://simply-debrid.com/generate";

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        final GetMethod method = getGetMethod(SD_GENERATE);
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            final HttpMethod postMethod = getMethodBuilder()
                    .setActionFromFormWhereActionContains("generate", false)
                    .setParameter("link", fileURL)
                    .setParameter("submit2", "Generate your links")
                    .setAction(SD_GENERATE)
                    .setReferer(SD_GENERATE)
                    .toPostMethod();
            if (!makeRequest(postMethod)) {
                throw new ServiceConnectionProblemException("Error posting link request");
            }
            final HttpMethod httpMethod = getGetMethod(SD_BASE_URL + PlugUtils.getStringBetween(getContentAsString(), "$.get(\"", "\","));
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final HttpMethod dlMethod = getGetMethod(PlugUtils.getStringBetween(getContentAsString(), "<a href=\"", "\">"));
            httpFile.setFileName(PlugUtils.getStringBetween(getContentAsString(), "\">", "</a>"));
            if (!tryDownloadAndSaveFile(dlMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Invalid link")) {
            throw new URLNotAvailableAnymoreException("Invalid link");
        }
        if (content.contains("This particular host is not available for non PREMIUM Members")) {
            throw new NotRecoverableDownloadException("This host is for Premium users only");
        }
        if (content.contains("Your free daily limit has been reached")) {
            throw new ErrorDuringDownloadingException("Your free daily limit has been reached");
        }
    }


    private void login() throws Exception {
        synchronized (Simply_DebridFileRunner.class) {
            Simply_DebridServiceImpl service = (Simply_DebridServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (pa.isSet()) {
                HttpMethod method = getGetMethod(SD_LOGIN);
                if (!makeRedirectedRequest(method)) { //we make the main request
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();//check problems
                method = getMethodBuilder()
                        .setActionFromFormWhereActionContains("password", true)
                        .setParameter("username", pa.getUsername())
                        .setParameter("password", pa.getPassword())
                        .setBaseURL(SD_BASE_URL)
                        .setReferer(SD_LOGIN)
                        .toPostMethod();
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException("Error posting login info");
                }
                if (getContentAsString().contains("Username or password invalid")) {
                    throw new BadLoginException("Invalid Simply-Debrid account login information!");
                }
            }

        }
    }

}