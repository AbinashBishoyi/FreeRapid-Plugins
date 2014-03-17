package cz.vity.freerapid.plugins.services.xfilesharing;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.CaptchaType;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.CaptchasCaptchaType;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.FourTokensCaptchaType;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.ReCaptchaType;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @author ntoskrnl
 */
public abstract class XFileSharingRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(XFileSharingRunner.class.getName());

    private final static Map<Class<?>, LoginData> LOGIN_CACHE = new WeakHashMap<Class<?>, LoginData>(2);

    private final List<CaptchaType> captchaTypes = getCaptchaTypes();

    protected List<CaptchaType> getCaptchaTypes() {
        final List<CaptchaType> captchaTypes = new LinkedList<CaptchaType>();
        captchaTypes.add(new ReCaptchaType());
        captchaTypes.add(new FourTokensCaptchaType());
        captchaTypes.add(new CaptchasCaptchaType());
        return captchaTypes;
    }

    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = new LinkedList<FileNameHandler>();
        fileNameHandlers.add(new FileNameHandlerA());
        fileNameHandlers.add(new FileNameHandlerB());
        fileNameHandlers.add(new FileNameHandlerC());
        return fileNameHandlers;
    }

    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = new LinkedList<FileSizeHandler>();
        fileSizeHandlers.add(new FileSizeHandlerA());
        fileSizeHandlers.add(new FileSizeHandlerB());
        fileSizeHandlers.add(new FileSizeHandlerC());
        fileSizeHandlers.add(new FileSizeHandlerD());
        return fileSizeHandlers;
    }

    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = new LinkedList<String>();
        downloadPageMarkers.add("File Download Link Generated");
        downloadPageMarkers.add("This direct link will be ");
        return downloadPageMarkers;
    }

    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = new LinkedList<String>();
        downloadLinkRegexes.add("<a href\\s?=\\s?(?:\"|')(http.+?" + Pattern.quote(httpFile.getFileName()) + ")(?:\"|')");
        return downloadLinkRegexes;
    }

    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        final MethodBuilder methodBuilder = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("method_free", true)
                .setAction(fileURL);
        if (!methodBuilder.getParameters().get("method_free").isEmpty()) {
            methodBuilder.removeParameter("method_premium");
        }
        return methodBuilder;
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setLanguageCookie();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkFileProblems();
            checkNameAndSize();
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        setLanguageCookie();
        logger.info("Starting download in TASK " + fileURL);
        login();
        HttpMethod method = getGetMethod(fileURL);
        int httpStatus = client.makeRequest(method, false);
        if (httpStatus / 100 == 3) {
            handleDirectDownload(method);
            return;
        }
        if (httpStatus != 200) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        checkFileProblems();
        checkNameAndSize();
        checkDownloadProblems();
        for (int loopCounter = 0; ; loopCounter++) {
            if (loopCounter >= 8) {
                //avoid infinite loops
                throw new PluginImplementationException("Cannot proceed to download link");
            }
            final MethodBuilder methodBuilder = getXFSMethodBuilder();
            final int waitTime = getWaitTime();
            final long startTime = System.currentTimeMillis();
            stepPassword(methodBuilder);
            if (!stepCaptcha(methodBuilder)) {                // skip the wait timer if its on the same page
                sleepWaitTime(waitTime, startTime);           //   as a captcha of type ReCaptcha
            }
            method = methodBuilder.toPostMethod();
            httpStatus = client.makeRequest(method, false);
            if (httpStatus / 100 == 3) {
                //redirect to download file location
                method = stepRedirectToFileLocation(method);
                break;
            } else if (checkDownloadPageMarker()) {
                //page containing download link
                final String downloadLink = getDownloadLinkFromRegexes();
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(downloadLink)
                        .toGetMethod();
                break;
            }
            checkDownloadProblems();
        }
        doDownload(method);
    }

    protected void doDownload(final HttpMethod method) throws Exception {
        setFileStreamContentTypes("text/plain");
        //some servers prefer to GZIP certain downloads, which we don't want
        method.removeRequestHeader("Accept-Encoding");
        if (!tryDownloadAndSaveFile(method)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    protected void handleDirectDownload(HttpMethod method) throws Exception {
        logger.info("Direct download");
        if (httpFile.getFileName() == null) {
            final Cookie[] cookies = client.getHTTPClient().getState().getCookies();
            client.getHTTPClient().getState().clearCookies();
            runCheck();
            client.getHTTPClient().getState().addCookies(cookies);
        }
        method = stepRedirectToFileLocation(method);
        doDownload(method);
    }

    protected String getCookieDomain() throws Exception {
        String host = new URL(getBaseURL()).getHost();
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }
        return "." + host;
    }

    protected void setLanguageCookie() throws Exception {
        addCookie(new Cookie(getCookieDomain(), "lang", "english", "/", 86400, false));
    }

    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        checkFileName();
        checkFileSize();
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    protected void checkFileName() throws ErrorDuringDownloadingException {
        for (final FileNameHandler fileNameHandler : getFileNameHandlers()) {
            try {
                fileNameHandler.checkFileName(httpFile, getContentAsString());
                logger.info("Name handler: " + fileNameHandler.getClass().getSimpleName());
                return;
            } catch (final ErrorDuringDownloadingException e) {
                //failed
            }
        }
        throw new PluginImplementationException("File name not found");
    }

    protected void checkFileSize() throws ErrorDuringDownloadingException {
        for (final FileSizeHandler fileSizeHandler : getFileSizeHandlers()) {
            try {
                fileSizeHandler.checkFileSize(httpFile, getContentAsString());
                logger.info("Size handler: " + fileSizeHandler.getClass().getSimpleName());
                return;
            } catch (final ErrorDuringDownloadingException e) {
                //failed
            }
        }
        throw new PluginImplementationException("File size not found");
    }

    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }

    protected void sleepWaitTime(final int waitTime, final long startTime) throws Exception {
        if (waitTime > 0) {
            //time taken to input password and captcha - no need to wait this time twice
            final int diffTime = (int) ((System.currentTimeMillis() - startTime) / 1000);
            if (waitTime > diffTime) {
                downloadTask.sleep(waitTime - diffTime);
            }
        }
    }

    protected void stepPassword(final MethodBuilder methodBuilder) throws Exception {
        if (getContentAsString().contains("<input type=\"password\" name=\"password\" class=\"myForm\">")) {
            final String serviceTitle = ((XFileSharingServiceImpl) getPluginService()).getServiceTitle();
            final String password = getDialogSupport().askForPassword(serviceTitle);
            if (password == null) {
                throw new NotRecoverableDownloadException("This file is secured with a password");
            }
            methodBuilder.setParameter("password", password);
        }
    }

    protected boolean stepCaptcha(final MethodBuilder methodBuilder) throws Exception {
        for (final CaptchaType captchaType : captchaTypes) {
            if (captchaType.canHandle(getContentAsString())) {
                logger.info("Captcha type: " + captchaType.getClass().getSimpleName());
                captchaType.handleCaptcha(methodBuilder, client, getCaptchaSupport());
                return (captchaType instanceof ReCaptchaType);
            }
        }
        return false;
    }

    protected boolean checkDownloadPageMarker() {
        for (final String downloadPageMarker : getDownloadPageMarkers()) {
            if (getContentAsString().contains(downloadPageMarker)) {
                return true;
            }
        }
        return false;
    }

    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        for (final String downloadLinkRegex : getDownloadLinkRegexes()) {
            final Matcher matcher = getMatcherAgainstContent(downloadLinkRegex);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        throw new PluginImplementationException("Download link not found");
    }

    protected HttpMethod stepRedirectToFileLocation(HttpMethod method) throws Exception {
        final Header locationHeader = method.getResponseHeader("Location");
        if (locationHeader == null) {
            throw new PluginImplementationException("Invalid redirect");
        }
        return getMethodBuilder()
                .setReferer(fileURL)
                .setAction(locationHeader.getValue())
                .toGetMethod();
    }

    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File Not Found")
                || content.contains("file was removed")
                || content.contains("file has been removed")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("server is in maintenance mode")) {
            throw new ServiceConnectionProblemException("This server is in maintenance mode. Please try again later.");
        }
    }

    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
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
        if (content.contains("have reached the download-limit")) {
            throw new YouHaveToWaitException("You have reached the download limit", 10 * 60);
        }
        if (content.contains("Error happened when generating Download Link")) {
            throw new YouHaveToWaitException("Error happened when generating download link", 60);
        }
        if (content.contains("Free Download Closed")) {
            throw new ServiceConnectionProblemException("Reached free download limit, wait or try premium");
        }
        if (content.contains("file is available to premium users only")
                || content.contains("this file requires premium to download")) {
            throw new NotRecoverableDownloadException("This file is only available to premium users");
        }
        if (content.contains("Wrong password")) {
            throw new ServiceConnectionProblemException("Wrong password");
        }
    }

    protected boolean login() throws Exception {
        synchronized (getClass()) {
            final PremiumAccount pa = ((XFileSharingServiceImpl) getPluginService()).getConfig();
            if (pa == null || !pa.isSet()) {
                LOGIN_CACHE.remove(getClass());
                logger.info("No account data set, skipping login");
                return false;
            }
            final LoginData loginData = LOGIN_CACHE.get(getClass());
            if (loginData == null || !pa.equals(loginData.getPa()) || loginData.isStale()) {
                logger.info("Logging in");
                doLogin(pa);
                final Cookie xfss = getCookieByName("xfss");
                if (xfss == null) {
                    throw new PluginImplementationException("Login cookie not found");
                }
                LOGIN_CACHE.put(getClass(), new LoginData(pa, xfss.getValue()));
            } else {
                logger.info("Login data cache hit");
                addCookie(new Cookie(getCookieDomain(), "xfss", loginData.getXfss(), "/", 86400, false));
            }
            return true;
        }
    }

    protected void doLogin(final PremiumAccount pa) throws Exception {
        HttpMethod method = getMethodBuilder()
                .setReferer(getBaseURL())
                .setAction(getBaseURL() + "/login.html")
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        method = getMethodBuilder()
                .setReferer(getBaseURL() + "/login.html")
                .setAction(getBaseURL())
                .setActionFromFormByName("FL", true)
                .setParameter("login", pa.getUsername())
                .setParameter("password", pa.getPassword())
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        if (getContentAsString().contains("Incorrect Login or Password")) {
            throw new BadLoginException("Invalid account login information");
        }
    }

    private static class LoginData {
        private final static long MAX_AGE = 86400000;//1 day
        private final long created;
        private final PremiumAccount pa;
        private final String xfss;

        public LoginData(final PremiumAccount pa, final String xfss) {
            this.created = System.currentTimeMillis();
            this.pa = pa;
            this.xfss = xfss;
        }

        public boolean isStale() {
            return System.currentTimeMillis() - created > MAX_AGE;
        }

        public PremiumAccount getPa() {
            return pa;
        }

        public String getXfss() {
            return xfss;
        }
    }

}