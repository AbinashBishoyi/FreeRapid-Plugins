package cz.vity.freerapid.plugins.services.filefactory;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda, ntoskrnl
 */
class FileFactoryRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(FileFactoryRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkPasswordProtected();
            checkSeriousProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkSeriousProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkPasswordProtected() throws Exception {
        while (getContentAsString().contains("Password Protected File")
                || getContentAsString().contains("File has been password protected")) {
            final String password = getDialogSupport().askForPassword("FileFactory");
            if (password == null) {
                throw new PluginImplementationException("This File has been password protected");
            }
            final HttpMethod method = getMethodBuilder()
                    .setActionFromFormWhereTagContains("password", true)
                    .setAction(fileURL).setReferer(fileURL)
                    .setParameter("password", password)
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                checkSeriousProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);
        login();
        if (makeRedirectedRequest(getMethod)) {
            checkPasswordProtected();
            checkAllProblems();
            checkNameAndSize(getContentAsString());

            if (getContentAsString().contains("Download with FileFactory TrafficShare")) {
                HttpMethod finalMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromAHrefWhereATagContains("Download with FileFactory TrafficShare")
                        .toGetMethod();
                if (tryDownloadAndSaveFile(finalMethod)) {
                    return;
                }
                makeRedirectedRequest(getMethod);
            }
            final HttpMethod finalMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(PlugUtils.getStringBetween(getContentAsString(), "data-href=\"", "\""))
                    .toGetMethod();

            downloadTask.sleep(PlugUtils.getWaitTimeBetween(getContentAsString(), "data-delay=\"", "\"", TimeUnit.SECONDS) + 1);

            if (!tryDownloadAndSaveFile(finalMethod)) {
                checkAllProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkAllProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(final String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("<div id=\"file_name\".*?>\\s*?<h2>(.+?)</h2>\\s*?<div id=\"file_info\".*?>\\s*?(.+?)\\s*?upload", content);
        if (!match.find())
            throw new PluginImplementationException("File name/size not found");
        httpFile.setFileName(match.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(2).trim()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Sorry, this file is no longer available") ||
                contentAsString.contains("the file you are requesting is no longer available") ||
                contentAsString.contains("file is no longer available") ||
                contentAsString.contains("This file has been deleted") ||
                contentAsString.contains("Invalid Download Link") ||
                contentAsString.contains("This file has been removed")) {
            throw new URLNotAvailableAnymoreException("Sorry, this file is no longer available. It may have been deleted by the uploader, or has expired");
        }
        if (contentAsString.contains("This file is forbidden to be shared")) {
            throw new URLNotAvailableAnymoreException("File is forbidden to be shared");
        }
        if (contentAsString.contains("What is FileFactory?")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
        if (contentAsString.contains("Sorry, there are currently no free download slots available on this server")
                || contentAsString.contains("All of the available ")
                || contentAsString.contains("All free download slots are in use")) {
            throw new YouHaveToWaitException("All free download slots are in use", 10 * 60);
        }
        if (contentAsString.contains("Server Maintenance") ||
                contentAsString.contains("The server hosting this file is temporarily unavailable")) {
            throw new ServiceConnectionProblemException("File's server currently down for maintenance");
        }
        if (contentAsString.contains("Server Load Too High") ||
                contentAsString.contains("The server hosting this file is temporarily overloaded")) {
            throw new ServiceConnectionProblemException("File's server is temporarily overloaded");
        }
        if (contentAsString.contains("Sorry, this file can only be downloaded by Premium members")
                || contentAsString.contains("this file can only be downloaded by FileFactory Premium")
                || contentAsString.contains("This file is only available to Premium Members")
                || contentAsString.contains("Premium Account Required")
                || contentAsString.contains("Please purchase an account to download this file")) {
            throw new NotRecoverableDownloadException("This file is only for Premium members");
        }
        if (contentAsString.contains("This file has been flagged as potentially containing content that contravenes FileFactory's policies")) {
            throw new NotRecoverableDownloadException("This file has been flagged as potentially containing content that contravenes FileFactory's policies");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();

        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Your download slot has expired")) {
            throw new ServiceConnectionProblemException("Your download slot has expired. Please try again");
        }
        if (contentAsString.contains("<h2>Over Capacity</h2>")) {
            throw new YouHaveToWaitException("FileFactory is currently experiencing high load and we are unable to service your request at this time", 120);
        }
        if (contentAsString.contains("You are currently downloading too many files at once")) {
            throw new ServiceConnectionProblemException("You are currently downloading too many files at once. Multiple simultaneous downloads are only permitted for Premium Members");
        }
        Matcher matcher = getMatcherAgainstContent("You(?:r IP)? \\((.+?)\\) (?:has|have) exceeded the download limit for free users");
        if (matcher.find()) {
            final String userIP = matcher.group(1);
            matcher = getMatcherAgainstContent("Please wait (.+?) (.+?) to download more files");
            int waitSeconds = 2 * 60;
            if (matcher.find()) {
                if (matcher.group(2).equals("minutes")) {
                    waitSeconds = 60 * Integer.parseInt(matcher.group(1));
                } else {
                    waitSeconds = Integer.parseInt(matcher.group(1));
                }
            }
            throw new YouHaveToWaitException(String.format("You (%s) have exceeded the download limit for free users", userIP), waitSeconds);
        }
        if (contentAsString.contains("You have recently started a download")) {
            Matcher match = getMatcherAgainstContent("Please try again in <span>(.+?), (.+?).</span>");
            int waitSeconds = 2 * 60;
            if (match.find()) {
                waitSeconds = 60 * Integer.parseInt(match.group(1).split(" ")[0]);
                waitSeconds += Integer.parseInt(match.group(2).split(" ")[0]);
            }
            throw new YouHaveToWaitException(String.format("You have exceeded the download limit for free users"), waitSeconds);
        }
    }


    private void login() throws Exception {
        synchronized (FileFactoryRunner.class) {
            final FileFactoryServiceImpl service = (FileFactoryServiceImpl) getPluginService();
            final PremiumAccount pa = service.getConfig();
            if (pa.isSet()) {
                final HttpMethod httpMethod = getMethodBuilder()
                        .setBaseURL("http://www.filefactory.com")
                        .setAction("/member/signin.php")
                        .setParameter("loginEmail", pa.getUsername())
                        .setParameter("loginPassword", pa.getPassword())
                        .setParameter("Submit", "Sign In")
                        .toPostMethod();
                if (!makeRedirectedRequest(httpMethod))
                    throw new ServiceConnectionProblemException("Error posting login info");

                if (getContentAsString().contains("The Email Address submitted was invalid") ||
                        getContentAsString().contains("Sign In Failed"))
                    throw new BadLoginException("Invalid FileFactory account login information!");
            }
        }
    }

}