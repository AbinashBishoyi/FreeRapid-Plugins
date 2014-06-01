package cz.vity.freerapid.plugins.services.ultramegabit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class UltraMegabitFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UltraMegabitFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkFileProblems();
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("<h4><[^>]+>\\s*(.+?)\\s*\\(([^\\)]+?)\\)<", content);
        if (!match.find())
            throw new PluginImplementationException("File Name/Size not found");
        httpFile.setFileName(match.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkFileProblems();
            checkProblems();
            checkNameAndSize(contentAsString);//extract file name and size from the page
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("recaptcha", true)
                    .toPostMethod();

            final int status = client.makeRequest(httpMethod, false);
            if (status / 100 == 3) {
                httpMethod = getGetMethod(httpMethod.getResponseHeader("Location").getValue());
            }
            httpMethod.removeRequestHeader("Accept-Encoding");  // stop it using gzip
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File Not Found")
                || content.contains("file was removed")
                || content.contains("File not available")
                || content.contains("file has been deleted")
                || content.contains("file has been removed")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("a database connection error has occur")) {
            throw new ServiceConnectionProblemException("A database connection error has occurred");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Account limitation notice") && content.contains("are only able to download")) {
            throw new YouHaveToWaitException("Download limit reached", 300);
        }
        if (content.contains("till next download") || content.contains("You have to wait")) {
            final Matcher matcher = getMatcherAgainstContent("(?:(\\d+) hours?, )?(?:(\\d+) minutes?, )?(?:(\\d+) seconds?)");
            int waitHours = 0, waitMinutes = 0, waitSeconds = 0;
            if (matcher.find()) {
                if (matcher.group(1) != null) {
                    waitHours = Integer.parseInt(matcher.group(1));
                }
                if (matcher.group(2) != null) {
                    waitMinutes = Integer.parseInt(matcher.group(2));
                }
                waitSeconds = Integer.parseInt(matcher.group(3));
            }
            final int waitTime = (waitHours * 60 * 60) + (waitMinutes * 60) + waitSeconds;
            throw new YouHaveToWaitException("You have to wait " + matcher.group(), waitTime);
        }
        if (content.contains("Undefined subroutine")) {
            throw new PluginImplementationException("Plugin is broken - Undefined subroutine");
        }
        if (content.contains("Skipped countdown")) {
            throw new PluginImplementationException("Plugin is broken - Skipped countdown");
        }
        if (content.contains("file reached max downloads limit")) {
            throw new ServiceConnectionProblemException("This file reached max downloads limit");
        }
        if (content.contains("You can download files up to")) {
            throw new NotRecoverableDownloadException(PlugUtils.getStringBetween(content, "<div class=\"err\">", "<br>"));
        }
        if (content.contains("Your download has been limited")) {
            final int wait = PlugUtils.getNumberBetween(content, "ts = (", " +") + 60 * 60;
            final long now = (new Date()).getTime() / 1000;
            final int delay = wait - (int) now;
            throw new YouHaveToWaitException("Your download has been limited, you need to wait " + (delay / 60) + ":" + (delay % 60), delay);
        }
        if (content.contains("have reached the download-limit") ||
                content.contains("Free Download Closed")) {
            throw new YouHaveToWaitException("Reached free download limit, wait or try premium", 10 * 60);
        }
        if (content.contains("There are too many free users downloading")) {
            throw new YouHaveToWaitException("Too many free users downloading from this server at this time", 10 * 60);
        }
        if (content.contains("Error happened when generating Download Link")) {
            throw new YouHaveToWaitException("Error happened when generating download link", 60);
        }
        if (content.contains("file is available to premium users only")
                || content.contains("Premium only download, Upgrade now")
                || content.contains("this file requires premium to download")) {
            throw new NotRecoverableDownloadException("This file is only available to premium users");
        }
    }


}