package cz.vity.freerapid.plugins.services.uploading;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.Calendar;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl, CapCap, Abinash Bishoyi, birchie
 */
class UploadingRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadingRunner.class.getName());
    private final static Random random = new Random(System.nanoTime());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".uploading.com", "setlang", "en", "/", 86400, false));
        addCookie(new Cookie(".uploading.com", "_lang", "en", "/", 86400, false));
        addCookie(new Cookie(".uploading.com", "lang", "1", "/", 86400, false));
        checkUrl();
        final HttpMethod method = getGetMethod(fileURL);
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
        addCookie(new Cookie(".uploading.com", "setlang", "en", "/", 86400, false));
        addCookie(new Cookie(".uploading.com", "_lang", "en", "/", 86400, false));
        addCookie(new Cookie(".uploading.com", "lang", "1", "/", 86400, false));
        checkUrl();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            method = getMethodBuilder()     // get wait time, which also sets a cookie 4 the download
                    .setReferer(fileURL)
                    .setAction("http://uploading.com/files/get/?ajax")
                    .setParameter("action", "second_page")
                    .setParameter("code", PlugUtils.getStringBetween(fileURL + "/", "get/", "/"))
                    .toPostMethod();
            method.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error getting wait time");
            }
            checkProblems();
            Matcher matcher = getMatcherAgainstContent("\"wait_time\"\\s*:\\s*\"?(\\d+)\"?");
            if (!matcher.find()) {
                throw new PluginImplementationException("Wait time not found");
            }
            downloadTask.sleep(Integer.parseInt(matcher.group(1)) + 1);

            method = getMethodBuilder()       // get link to download page
                    .setReferer(fileURL)
                    .setAction("http://uploading.com/files/get/?ajax")
                    .setParameter("action", "get_link")
                    .setParameter("code", PlugUtils.getStringBetween(fileURL + "/", "get/", "/"))
                    .setParameter("pass", "false")
                    .setParameter("force_exe", "0") //disable download manager
                    .toPostMethod();
            method.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error getting link to download page");
            }
            checkProblems();
            matcher = getMatcherAgainstContent("\"link\"\\s*?:\\s*?\"(http.+?)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Download page link not found");
            }
            method = getGetMethod((matcher.group(1).replace("\\/", "/")));
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error getting thanks page");
            }
            checkProblems();
            matcher = getMatcherAgainstContent("document\\.location\\.href='(http.+?)'");
            if (!matcher.find()) {
                throw new PluginImplementationException("Thanks page not found");
            }
            method = getGetMethod((matcher.group(1).replace("\\/", "/")));
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error getting download page");
            }
            checkProblems();
            matcher = getMatcherAgainstContent("\"file_form\"\\s+?action=\"(http.+?)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Download link not found");
            }
            method = getGetMethod((matcher.group(1).replace("\\/", "/")));

            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkUrl() {
        if (!fileURL.contains("/get")) {
            fileURL = fileURL.replaceFirst("/files", "/files/get");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("Filemanager</a></li>\\s*?<li>(.+?)</li>", getContentAsString());
        if (!match.find())
            throw new PluginImplementationException("File name not found");
        httpFile.setFileName(match.group(1).trim());
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<li class=\"size tip_container\">", "<div");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Your IP address is currently downloading")) {
            throw new ServiceConnectionProblemException("Your IP address is currently downloading a file.\nPlease wait until the downloading process has been completed.");
        }
        if (getContentAsString().contains("Sorry, but file you are trying to download is larger then allowed for free download.")) {
            throw new ServiceConnectionProblemException("Sorry, but file you are trying to download is larger then allowed for free download.");
        }
        if (getContentAsString().contains("You still need to wait for the start of your download")) {
            throw new YouHaveToWaitException("You still need to wait for the start of your download", 65);
        }
        if (getContentAsString().contains("Requested file not found") ||
                getContentAsString().contains("the file was removed") ||
                getContentAsString().contains("Looks like file not found")) {
            throw new URLNotAvailableAnymoreException("Requested file not found");
        }
        if (getContentAsString().contains("You have reached the daily downloads limit")) {
            int pause = 20 * 60;
            int toMidnight = getSecondToMidnight();
            if (toMidnight > 18 * 3600) pause = toMidnight + 5 * 60;

            throw new YouHaveToWaitException("You have reached the daily downloads limit. Please come back later", pause);
        }
        if (getContentAsString().contains("The requested file is not found")) {
            throw new URLNotAvailableAnymoreException("Requested file not found");
        }
        if (getContentAsString().contains("Service Not Available")) {
            throw new YouHaveToWaitException("Service Not Available For Now", random.nextInt(10));
        }
        if (getContentAsString().contains("Download Limit")) {
            Matcher matcher = getMatcherAgainstContent("Sorry[\\D]*(\\d)* minutes");
            int pause = 5;
            if (matcher.find()) {
                pause = Integer.parseInt(matcher.group(1));
            }
            throw new YouHaveToWaitException("Download Limit", 60 * pause);
        }
    }

    private static int getSecondToMidnight() {
        Calendar now = Calendar.getInstance();
        Calendar midnight = Calendar.getInstance();
        midnight.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH) + 1, 0, 0, 1);
        return (int) ((midnight.getTimeInMillis() - now.getTimeInMillis()) / 1000f);
    }

}