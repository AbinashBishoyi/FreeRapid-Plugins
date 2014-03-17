package cz.vity.freerapid.plugins.services.ryushare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class RyuShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RyuShareFileRunner.class.getName());
    private static final String SERVICE_TITLE = "RyuShare";
    private static final String SERVICE_COOKIE_DOMAIN = ".ryushare.com";
    private final static String SERVICE_LOGIN_REFERER = "http://www.ryushare.com/login.python";
    private final static String SERVICE_LOGIN_ACTION = "http://www.ryushare.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "english", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "Download File", "</h2>");
        PlugUtils.checkFileSize(httpFile, content, "</font> (", ")</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean login() throws Exception {
        synchronized (RyuShareFileRunner.class) {
            RyuShareServiceImpl service = (RyuShareServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();

            //for testing purpose
            //pa.setPassword("freerapid");
            //pa.setUsername("freerapid");

            if (pa == null || !pa.isSet()) {
                logger.info("No account data set, skipping login");
                return false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(SERVICE_LOGIN_REFERER)
                    .setAction(SERVICE_LOGIN_ACTION)
                    .setParameter("op", "login")
                    .setParameter("redirect", "")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("loginFormSubmit", "")
                    .toPostMethod();
            addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "login", pa.getUsername(), "/", null, false));
            addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "xfss", "", "/", null, false));
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");
            if (getContentAsString().contains("Incorrect Login or Password"))
                throw new BadLoginException("Invalid " + SERVICE_TITLE + " registered account login information!");

            return true;
        }
    }

    private boolean isPassworded() {
        return getContentAsString().contains("<input type=\"password\" name=\"password\" class=\"myForm\">");
    }


    @Override
    public void run() throws Exception {
        super.run();
        login();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "english", "/", 86400, false));
        GetMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        checkFileProblems();
        checkNameAndSize(getContentAsString());

        HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("method_free", true)
                .setAction(fileURL)
                .removeParameter("method_premium")
                .toPostMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
        checkDownloadProblems();

        final MethodBuilder methodBuilder = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormByName("F1", true)
                .setAction(fileURL)
                .removeParameter("method_premium");

        String waitTimeRule = "id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span";
        Matcher waitTimematcher = PlugUtils.matcher(waitTimeRule, getContentAsString());
        if (waitTimematcher.find()) {
            downloadTask.sleep(Integer.parseInt(waitTimematcher.group(1)));
        }
        if (isPassworded()) {
            final String password = getDialogSupport().askForPassword(SERVICE_TITLE);
            if (password == null) {
                throw new NotRecoverableDownloadException("This file is secured with a password");
            }
            methodBuilder.setParameter("password", password);
        }

        httpMethod = methodBuilder.toPostMethod();
        final int httpStatus = client.makeRequest(httpMethod, false);
        if (httpStatus / 100 == 3) { //redirect to download file
            final Header locationHeader = httpMethod.getResponseHeader("Location");
            if (locationHeader == null)
                throw new ServiceConnectionProblemException("Could not find download file location");
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(locationHeader.getValue())
                    .toGetMethod();
        } else if (getContentAsString().contains("File Download Link Generated") || getContentAsString().contains("This direct link will be available for your IP")) { //link generated
            final Matcher downloadLinkMatcher = getMatcherAgainstContent("<a href=\"(http.+?" + httpFile.getFileName() + ")\">");
            if (!downloadLinkMatcher.find()) {
                throw new PluginImplementationException("Could not find generated download link");
            }
            final String downloadLink = downloadLinkMatcher.group(1);
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadLink)
                    .toGetMethod();
        }

        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("server is in maintenance mode")) {
            throw new YouHaveToWaitException("This server is in maintenance mode", 10 * 60);
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("till next download")) {
            String regexRule = "(?:([0-9]+) hours?, )?(?:([0-9]+) minutes?, )?(?:([0-9]+) seconds?) till next download";
            Matcher matcher = PlugUtils.matcher(regexRule, contentAsString);
            int waitHours = 0, waitMinutes = 0, waitSeconds = 0, waitTime;
            if (matcher.find()) {
                if (matcher.group(1) != null)
                    waitHours = Integer.parseInt(matcher.group(1));
                if (matcher.group(2) != null)
                    waitMinutes = Integer.parseInt(matcher.group(2));
                waitSeconds = Integer.parseInt(matcher.group(3));
            }
            waitTime = (waitHours * 60 * 60) + (waitMinutes * 60) + waitSeconds;
            throw new YouHaveToWaitException("You have to wait " + waitTime + " seconds", waitTime);
        }
        if (contentAsString.contains("Undefined subroutine")) {
            throw new PluginImplementationException("Server problem");
        }
        if (contentAsString.contains("file reached max downloads limit")) {
            throw new PluginImplementationException("This file reached max downloads limit");
        }
        if (contentAsString.contains("You can download files up to")) {
            throw new PluginImplementationException(PlugUtils.getStringBetween(contentAsString, "<div class=\"err\">", ".<br>"));
        }
        if (contentAsString.contains("have reached the download-limit")) {
            throw new YouHaveToWaitException("You have reached the download-limit", 10 * 60);
        }
        if (contentAsString.contains("Error happened when generating Download Link")) {
            throw new YouHaveToWaitException("Error happened when generating Download Link", 60);
        }
        if (contentAsString.contains("file is available to premium users only")) {
            throw new PluginImplementationException("This file is available to premium users only");
        }
        if (contentAsString.contains("this file requires premium to download")) {
            throw new PluginImplementationException("This file is available to premium users only");
        }
    }

}