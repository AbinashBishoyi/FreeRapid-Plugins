package cz.vity.freerapid.plugins.services.filefactory_premium;

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
class FileFactoryFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(FileFactoryFileRunner.class.getName());
    private static final String SERVICE_WEB = "http://www.filefactory.com";
    private boolean badConfig = false;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod method = getGetMethod(fileURL);
        int httpStatus = client.makeRequest(method, false);
        if (httpStatus / 100 == 3) {    // direct download
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else if (httpStatus == 200) {
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
        int httpStatus = client.makeRequest(method, false);
        if (httpStatus / 100 == 3) {    // direct download
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else if (httpStatus == 200) {
            checkProblems();
            checkNameAndSize();

            if (getContentAsString().contains("Download with FileFactory Basic"))
                throw new BadLoginException("Problem logging in, account not premium?");

            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("Download with FileFactory")
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("<div id=\"file_name\".*?>\\s*?<h2>(.+?)</h2>\\s*?<div id=\"file_info\".*?>\\s*?(.+?)\\s*?upload", getContentAsString());
        if (!match.find())
            throw new PluginImplementationException("File name/size not found");
        httpFile.setFileName(match.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(2).trim()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Sorry, this file is no longer available") ||
                content.contains("the file you are requesting is no longer available") ||
                content.contains("This file has been deleted") ||
                content.contains("Invalid Download Link") ||
                content.contains("This file has been removed")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("You have presently exceeded your Download allowance")) {
            final Matcher matcher = getMatcherAgainstContent("reset in (\\d+?) hour");
            if (!matcher.find())
                throw new PluginImplementationException("You have presently exceeded your download allowance, but waiting time was not found");
            throw new YouHaveToWaitException("You have presently exceeded your download allowance", 3600 * Integer.valueOf(matcher.group(1)) + 60 * 5);
        }
        if (content.contains("Server Maintenance") ||
                content.contains("The server hosting this file is temporarily unavailable")) {
            throw new ServiceConnectionProblemException("File's server currently down for maintenance");
        }
        if (content.contains("Server Load Too High") ||
                content.contains("The server hosting this file is temporarily overloaded")) {
            throw new ServiceConnectionProblemException("File's server is temporarily overloaded");
        }
    }

    private void login() throws Exception {
        synchronized (FileFactoryFileRunner.class) {
            FileFactoryServiceImpl service = (FileFactoryServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet() || badConfig) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No FileFactory Premium account login information!");
                }
                badConfig = false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setBaseURL(SERVICE_WEB)
                    .setAction("/member/signin.php")
                    .setParameter("loginEmail", pa.getUsername())
                    .setParameter("loginPassword", pa.getPassword())
                    .setParameter("Submit", "Sign In")
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");

            if (getContentAsString().contains("The Email Address submitted was invalid") ||
                    getContentAsString().contains("Sign In Failed"))
                throw new BadLoginException("Invalid FileFactory Premium account login information!");
        }
    }

}