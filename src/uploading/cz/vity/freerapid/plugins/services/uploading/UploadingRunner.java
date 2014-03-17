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
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
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

            method = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("downloadform", true).toPostMethod();

            if (makeRedirectedRequest(method)) {
                checkProblems();

                //      final String fileId = PlugUtils.getParameter("file_id", getContentAsString());
                if (getContentAsString().contains("timeadform")) {
                    method = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("timeadform", true).toPostMethod();
                    int wait;
                    try {
                        wait = PlugUtils.getNumberBetween(getContentAsString(), "start_timer(", ")");
                    } catch (PluginImplementationException e) {
                        wait = PlugUtils.getNumberBetween(getContentAsString(), "timead_counter\">", "<");
                    }
                    downloadTask.sleep(wait + 1);
                    if (!makeRedirectedRequest(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                }
                try {
                    downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "start_timer(", ")") + 1);
                } catch (PluginImplementationException e) {
                    //no waiting time
                }
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction("http://uploading.com/files/get/?JsHttpRequest=" + System.currentTimeMillis() + "-xml")
                                //.setParameter("file_id", fileId)
                        .setParameter("pass", "")
                        .setParameter("code", PlugUtils.getStringBetween(fileURL, "get/", "/"))
                        .setParameter("action", "get_link")
                        .toGetMethod();
                if (makeRedirectedRequest(method)) {
                    checkProblems();
                    final Matcher matcher = getMatcherAgainstContent("\"link\"\\s*?:\\s*?\"(http.+?)\"");
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Download link not found");
                    }
                    method = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1).replace("\\/", "/")).toGetMethod();
                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                } else {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
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
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "File size: <b>", "</b> <br/>");
        PlugUtils.checkName(httpFile, getContentAsString(), "<h2>", "</h2><br/>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Your IP address is currently downloading")) {
            throw new ServiceConnectionProblemException("Your IP address is currently downloading a file.\nPlease wait until the downloading process has been completed.");
        }
        if (getContentAsString().contains("You still need to wait for the start of your download")) {
            throw new YouHaveToWaitException("You still need to wait for the start of your download", 65);
        }
        if (getContentAsString().contains("Requested file not found")) {
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
            //  throw new ServiceConnectionProblemException("Service Not Available");
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