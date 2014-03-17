package cz.vity.freerapid.plugins.services.jalurcepat;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class JalurCepatFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(JalurCepatFileRunner.class.getName());
    private static final String SERVICE_TITLE = "JalurCepat";
    private static final String SERVICE_COOKIE_DOMAIN = ".jalurcepat.com";

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
        PlugUtils.checkName(httpFile, content, "File Name:</b></td><td nowrap>", "</b>");
        PlugUtils.checkFileSize(httpFile, content, "File Size:</b></td><td>", "<small>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "english", "/", 86400, false));
        GetMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        checkFileProblems();
        checkNameAndSize(getContentAsString());

        final String waitTimeRule = "id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span";
        final Matcher waitTimematcher = PlugUtils.matcher(waitTimeRule, getContentAsString());
        if (waitTimematcher.find()) {
            downloadTask.sleep(Integer.parseInt(waitTimematcher.group(1)));
        }
        String password = "";
        if (isPassworded()) {
            password = getDialogSupport().askForPassword(SERVICE_TITLE);
            if (password == null) {
                throw new NotRecoverableDownloadException("This file is secured with a password");
            }
        }
        MethodBuilder methodBuilder = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormByName("F1", true)
                .setAction(fileURL)
                .removeParameter("method_premium")
                .setParameter("code", stepCaptcha());
        if (isPassworded()) {
            methodBuilder.setParameter("password", password);
        }
        HttpMethod httpMethod = methodBuilder.toPostMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
        checkDownloadProblems();
        httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromAHrefWhereATagContains("downloadhere.gif")
                .toGetMethod();
        setFileStreamContentTypes("text/plain");
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private boolean isPassworded() {
        return getContentAsString().contains("<input type=\"password\" name=\"password\" class=\"myForm\">");
    }

    private String stepCaptcha() throws Exception {
        CaptchaSupport captchaSupport = getCaptchaSupport();
        String captchaURL = "http://www.jalurcepat.com/captchas/" + PlugUtils.getStringBetween(getContentAsString(), "<img src=\"http://www.jalurcepat.com/captchas/", "\">");
        logger.info("Captcha URL " + captchaURL);
        String captcha = captchaSupport.getCaptcha(captchaURL);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        logger.info("Captcha : " + captcha);
        return captcha;
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found") || contentAsString.contains("No such file") || contentAsString.contains("<font class=\"err\">")) {
            throw new URLNotAvailableAnymoreException("File not found");
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
        if (contentAsString.contains("Wrong captcha")) {
            throw new YouHaveToWaitException("Wrong captcha", 1);
        }
        if (contentAsString.contains("this file reached max downloads limit")) {
            throw new PluginImplementationException("Sorry but this file reached max downloads limit");
        }
    }

}