package cz.vity.freerapid.plugins.services.freakshare_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FreakShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FreakShareFileRunner.class.getName());
    private final static String LOGIN_URL = "http://freakshare.com/login.html";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
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
        final Matcher matcher = getMatcherAgainstContent("<h1 class=\"box_heading\"[^<>]+?>(.+?) - (.+?)</");
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
        login();

        final GetMethod method = getGetMethod(fileURL); //create GET request
        final int status = client.makeRequest(method, false);
        if (status / 100 == 3) {
            final String dlLink = method.getResponseHeader("Location").getValue();
            httpFile.setFileName(URLDecoder.decode(dlLink.substring(1 + dlLink.lastIndexOf("/")), "UTF-8"));
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else if (status == 200) {
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("Download", true)
                    .toPostMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("<h1[^<>]*>[^<>]*Error[^<>]*</h1>[^<>]*<div[^<>]*>([^<>]*)");
        if (matcher.find()) {
            String detail = matcher.group(1);
            if (PlugUtils.find("does.?n.?t\\s+exist", detail))
                throw new URLNotAvailableAnymoreException(detail);
            if (PlugUtils.find("can.?t\\s+download\\s+more\\s+th.n", detail))
                throw new YouHaveToWaitException(detail, 1800); // wait 30 minutes (as we have no way to tell how long)
            throw new NotRecoverableDownloadException(detail);
        }
    }


    private void login() throws Exception {
        synchronized (FreakShareFileRunner.class) {
            FreakShareServiceImpl service = (FreakShareServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No FreakShare account login information!");
                }
            }
            final HttpMethod method = getMethodBuilder()
                    .setAction(LOGIN_URL).setReferer(LOGIN_URL)
                    .setParameter("user", pa.getUsername())
                    .setParameter("pass", pa.getPassword())
                    .setParameter("submit", "Login")
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            if (getContentAsString().contains("Wrong Username or Password!")) {
                throw new BadLoginException("Invalid FreakShare account login information!");
            }
        }
    }

}
