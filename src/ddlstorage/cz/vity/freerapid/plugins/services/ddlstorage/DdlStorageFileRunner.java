package cz.vity.freerapid.plugins.services.ddlstorage;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
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
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class DdlStorageFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DdlStorageFileRunner.class.getName());
    private static final String SERVICE_TITLE = "DdlStorage";
    private static final String SERVICE_COOKIE_DOMAIN = ".ddlstorage.com";
    private static final String SERVICE_LOGIN_REFERER = "http://www.ddlstorage.com/login.html";
    private static final String SERVICE_LOGIN_ACTION = "http://www.ddlstorage.com";

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
        PlugUtils.checkName(httpFile, content, "\"fname\" value=\"", "\">");
        PlugUtils.checkFileSize(httpFile, content, httpFile.getFileName() + " (", ")</h2>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }


    private boolean login() throws Exception {
        synchronized (DdlStorageFileRunner.class) {
            DdlStorageServiceImpl service = (DdlStorageServiceImpl) getPluginService();
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
                    .setParameter("submit", "")
                    .toPostMethod();
            addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "login", pa.getUsername(), "/", null, false));
            addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "xfss", "", "/", null, false));
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");
            if (getContentAsString().contains("Incorrect Login or Password"))
                throw new BadLoginException("Invalid " + SERVICE_TITLE + "registered account login information!");

            return true;
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        login();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "english", "/", 86400, false));
        GetMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            logger.warning(getContentAsString());
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        checkFileProblems();
        checkNameAndSize(getContentAsString());

        HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setBaseURL(fileURL)
                .setActionFromFormWhereTagContains("method_free", true)
                .removeParameter("method_premium")
                .toPostMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkDownloadProblems();
            logger.warning(getContentAsString());
            throw new ServiceConnectionProblemException();
        }
        checkDownloadProblems();

        String regexp = "<div id=\"countdown_str\".+?<h2.*?>+\\s*(\\d+)\\s*</h2>";
        Matcher waitTimematcher = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL).matcher(getContentAsString());
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
        while (true) {
            MethodBuilder methodBuilder = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormByName("F1", true)
                    .setAction(fileURL)
                    .removeParameter("method_premium");
            if (isPassworded()) {
                methodBuilder.setParameter("password", password);
            }
            httpMethod = stepCaptcha(methodBuilder);
            final int httpStatus = client.makeRequest(httpMethod, false);
            if (httpStatus / 100 == 3) { //redirect
                final Header locationHeader = httpMethod.getResponseHeader("Location");
                if (locationHeader == null)
                    throw new ServiceConnectionProblemException("Could not find download file location");
                httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(locationHeader.getValue())
                        .toGetMethod();
                break;
            } else if (getContentAsString().contains("File Download Link Generated")){ //link generated
                httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromAHrefWhereATagContains(httpFile.getFileName())
                        .toGetMethod();
                break;
            } else {
                if (!getContentAsString().contains("recaptcha/api/challenge")) {
                    checkDownloadProblems();
                    throw new PluginImplementationException("Recaptcha not found");
                }
            }
        }                                    
        setFileStreamContentTypes("text/plain");
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private boolean isPassworded() {
        return getContentAsString().contains("<input type=\"password\" name=\"password\" class=\"myForm\">");
    }

    private HttpMethod stepCaptcha(MethodBuilder methodBuilder) throws Exception {
        final Matcher reCaptchaKeyMatcher = getMatcherAgainstContent("recaptcha/api/challenge\\?k=(.*?)\">");
        if (!reCaptchaKeyMatcher.find())
            throw new PluginImplementationException("Recaptcha not found");
        final String reCaptchaKey = reCaptchaKeyMatcher.group(1);
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);

        return r.modifyResponseMethod(methodBuilder).toPostMethod();
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
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
        if (contentAsString.contains("this file reached max downloads limit")) {
            throw new PluginImplementationException("Sorry but this file reached max downloads limit");
        }
    }

}