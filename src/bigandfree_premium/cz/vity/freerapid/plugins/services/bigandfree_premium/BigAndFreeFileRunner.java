package cz.vity.freerapid.plugins.services.bigandfree_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class BigAndFreeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BigAndFreeFileRunner.class.getName());
    private boolean badConfig = false;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod method = getGetMethod(fileURL);
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

        login();

        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            if (getContentAsString().contains("value=\"Basic Download\""))
                throw new NotRecoverableDownloadException("Problem logging in, account not premium?");

            final Matcher matcher = getMatcherAgainstContent("name=\"current\" value=\"([^\"]+?)\"");
            if (!matcher.find()) throw new PluginImplementationException("Download link issue");

            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(fileURL)
                    .setParameter("current", matcher.group(1))
                    .setParameter("limit_reached", "0")
                    .setParameter("download_now", "Click+here+to+download")
                    .toPostMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                if (getContentAsString().contains("<title>Big & Free - Download area</title>"))
                    throw new PluginImplementationException("Download link issue");
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "File Name: </font><font class=\"type3\">", "</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("The file you requested has been removed") || content.contains("<font class=\"type3\">N/A</font>") || content.contains("<h1>Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void login() throws Exception {
        synchronized (BigAndFreeFileRunner.class) {
            BigAndFreeServiceImpl service = (BigAndFreeServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet() || badConfig) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No BigAndFree Premium account login information!");
                }
                badConfig = false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction("http://www.bigandfree.com/members")
                    .setAndEncodeParameter("uname", pa.getUsername())
                    .setAndEncodeParameter("pwd", pa.getPassword())
                    .setParameter("remember", "on")
                    .setParameter("login", "Click+here+to+login")
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");

            if (getContentAsString().contains("Account not found"))
                throw new NotRecoverableDownloadException("Invalid BigAndFree Premium account login information!");
            if (!getContentAsString().contains("<font class=\"type3\">PREMIUM"))
                throw new NotRecoverableDownloadException("Account is not premium!");
        }
    }

}