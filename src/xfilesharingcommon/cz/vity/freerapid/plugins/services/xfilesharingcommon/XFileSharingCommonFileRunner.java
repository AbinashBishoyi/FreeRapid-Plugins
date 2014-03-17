package cz.vity.freerapid.plugins.services.xfilesharingcommon;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
//public abstract class XFileSharingCommonFileRunner extends AbstractRunner {
public class XFileSharingCommonFileRunner extends AbstractRunner {
    protected final static Logger logger = Logger.getLogger(XFileSharingCommonFileRunner.class.getName());
    private final int captchaMax = 8;
    private int captchaCounter = 0;
    /*
    protected abstract void checkNameAndSize(String content) throws ErrorDuringDownloadingException;

    protected abstract String getCookieDomain();

    protected abstract String getServiceTitle();

    protected abstract boolean isRegisteredUserImplemented();

    //protected abstract Class getRunnerClass();

    //protected abstract Class getImplClass();
    */

    //private void checkPrerequisites() throws PluginImplementationException {

    //check the prerequisites

    protected void checkPrerequisites() throws PluginImplementationException {
        if (getCookieDomain() == null)
            throw new PluginImplementationException("getCookieDomain return value cannot be null.");
        if (getServiceTitle() == null)
            throw new PluginImplementationException("getServiceTitle return value cannot be null.");
        if (isRegisteredUserImplemented()) {
            if (getLoginActionURL() == null)
                throw new PluginImplementationException("getLoginActionURL return value cannot be null.");
            if (getLoginURL() == null)
                throw new PluginImplementationException("getLoginURL return value cannot be null.");
            if (getRunnerClass() == null)
                throw new PluginImplementationException("getRunnerClass return value cannot be null.");
            if (getImplClass() == null)
                throw new PluginImplementationException("getImplClass return value cannot be null.");
        }
        if ((getNumberOfPages() < 1) || (getNumberOfPages() > 2))
            throw new PluginImplementationException("Number of pages should be 1 or 2.");
        if (useCustomCaptcha()) {
            if (getCustomCaptchaRegex() == null)
                throw new PluginImplementationException("getCustomCaptchaRegex return value cannot be null");
        }
    }

    //should be overrided.
    //if filename and size doesn't need to be checked, simply type : httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    protected void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        throw new PluginImplementationException("checkNameAndSize should be overrided");
    }

    //used to set language and in login related cookie
    //return value should not be null
    //return value ex : ".ryushare.com"
    protected String getCookieDomain() {
        return null;
    }

    //return value should not be null. used in password entry and login exception
    //return value ex : "RyuShare"
    protected String getServiceTitle() {
        return null;
    }

    //flag for registered user support
    protected boolean isRegisteredUserImplemented() {
        return false;
    }

    //return value should not be null, if registered user is supported
    //return value ex : RyuShareFileRunner.class;
    protected Class getRunnerClass() {
        return null;
    }

    //return value should not be null, if registered user is supported
    //return value ex : RyuShareServiceImpl.class;
    protected Class getImplClass() {
        return null;
    }

    //return value should be 1 or 2
    //mostly there are 2 pages that contains 'method_free' in FORM tag,
    //but some sites only show 1 page that contains 'method_free' in FORM tag 
    protected byte getNumberOfPages() {
        return 2;
    }

    //return value should not be null, if registered user is supported
    //return value ex : "http://www.ryushare.com/login.python
    //                  "http://www.ddlstorage.com/login.html
    protected String getLoginURL() {
        return null;
    }

    //return value should not be null, if registered user is supported
    //return value ex : "http://www.ryushare.com"
    protected String getLoginActionURL() {
        return null;
    }

    protected String getWaitTimeRegex() {
        return "id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span";
    }

    protected String getReCaptchaRegex() {
        return "recaptcha/api/challenge\\?k=(.*?)\"";
    }

    protected String getFourTokensCaptchaRegex() {
        return "<span style='position:absolute;padding\\-left:(\\d+)px;padding\\-top:\\d+px;'>(.+?)</span>";
    }

    //text inside IMG tag in 'captchas' image captcha
    protected String getCaptchasImgTagContains() {
        return "/captchas/";
    }

    protected boolean isCaptchaExistInContent(String content) {
        String captchaRegex = "(" + getReCaptchaRegex() + "|" + getFourTokensCaptchaRegex() + "|" + Pattern.quote(getCaptchasImgTagContains());
        if (useCustomCaptcha()) captchaRegex = captchaRegex + "|" + getCustomCaptchaRegex();
        captchaRegex = captchaRegex + ")";
        return PlugUtils.find(captchaRegex, content);
    }

    protected boolean isPassworded(String content) {
        return content.contains("<input type=\"password\" name=\"password\" class=\"myForm\">");
    }

    //regex of sample text that found in download link page
    protected String getDownloadLinkPageRegex() {
        return "(File Download Link Generated|This direct link will be available for your IP)";
    }

    protected String getDownloadLinkURLRegex() {
        return "<a href=\"(http.+?" + Pattern.quote(httpFile.getFileName()) + ")\"";
    }

    //if return value = true, leading '0's will be omitted in stepFourTokensCaptcha()
    protected boolean omitFourTokensCaptchaLeadingZero() {
        return true;
    }

    //if return value = true, custom captcha regex (getCustomCaptchaRegex()) will be checked, if found : stepCustomCaptcha() will be executed
    protected boolean useCustomCaptcha() {
        return false;
    }

    //return value should not be null, if custom captcha is supported
    protected String getCustomCaptchaRegex() {
        return null;
    }

    //if return value = true, customRun() will be executed
    protected boolean useCustomRun() {
        return false;
    }

    //set language cookie, checkPrerequisites(), and login() (if needed) are already handled by run()
    protected void customRun() throws Exception {
        throw new PluginImplementationException("customRun should be overrided");
    }

    //if return value = true, doWaitTime() will be executed for every captcha retry
    protected boolean waitTimeCaptchaRetry() {
        return false;
    }

    protected void doWaitTime() throws InterruptedException {
        final Matcher waitTimematcher = PlugUtils.matcher(getWaitTimeRegex(), getContentAsString());
        if (waitTimematcher.find()) {
            downloadTask.sleep(Integer.parseInt(waitTimematcher.group(1)) + 1);
        }
    }

    protected boolean login() throws Exception {
        if (!isRegisteredUserImplemented()) //registered user support flag check
            throw new PluginImplementationException("isRegisteredUserImplemented return value should be TRUE");
        synchronized (getRunnerClass()) {
            Method getConfig = getImplClass().getMethod("getConfig");
            PremiumAccount pa = (PremiumAccount) getConfig.invoke(getPluginService());

            if (pa == null || !pa.isSet()) {
                logger.info("No account data set, skipping login");
                return false;
            }
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(getLoginURL())
                    .setAction(getLoginActionURL())
                    .setParameter("op", "login")
                    .setParameter("redirect", "")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("submit", "")
                    .toPostMethod();
            addCookie(new Cookie(getCookieDomain(), "login", pa.getUsername(), "/", null, false));
            addCookie(new Cookie(getCookieDomain(), "xfss", "", "/", null, false));
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");
            if (getContentAsString().contains("Incorrect Login or Password"))
                throw new BadLoginException("Invalid " + getServiceTitle() + " registered account login information!");
            return true;
        }
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkPrerequisites();
        addCookie(new Cookie(getCookieDomain(), "lang", "english", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    //@TODO : premium account support
    @Override
    public void run() throws Exception {
        super.run();
        checkPrerequisites();
        addCookie(new Cookie(getCookieDomain(), "lang", "english", "/", 86400, false));
        if (isRegisteredUserImplemented()) login();
        if (useCustomRun()) {
            customRun();
            return;
        }
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        checkFileProblems();
        checkNameAndSize(getContentAsString());

        HttpMethod httpMethod;
        if (getNumberOfPages() == 2) {
            httpMethod = getMethodBuilder()
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
        }

        doWaitTime();
        String password = null;
        if (isPassworded(getContentAsString())) {
            password = getDialogSupport().askForPassword(getServiceTitle());
            if (password == null) {
                throw new NotRecoverableDownloadException("This file is secured with a password");
            }
        }
        final boolean captchaExist = isCaptchaExistInContent(getContentAsString());

        while (true) {
            MethodBuilder methodBuilder = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("method_free", true)
                    .setAction(fileURL)
                    .removeParameter("method_premium");
            if (isPassworded(getContentAsString())) {
                methodBuilder.setParameter("password", password);
            }
            if (PlugUtils.find(getReCaptchaRegex(), getContentAsString())) {
                httpMethod = stepReCaptcha(methodBuilder);
            } else if (PlugUtils.find(getFourTokensCaptchaRegex(), getContentAsString())) {
                methodBuilder.setParameter("code", stepFourTokensCaptcha());
                httpMethod = methodBuilder.toPostMethod();
            } else if (getContentAsString().contains(getCaptchasImgTagContains())) {
                methodBuilder.setParameter("code", stepCaptchas());
                httpMethod = methodBuilder.toPostMethod();
            } else if (useCustomCaptcha() && PlugUtils.find(getCustomCaptchaRegex(), getContentAsString())) {
                httpMethod = stepCustomCaptcha(methodBuilder);
            } else { //no captcha found
                httpMethod = methodBuilder.toPostMethod();
            }
            final int httpStatus = client.makeRequest(httpMethod, false);
            if (httpStatus / 100 == 3) { //redirect to download file location
                final Header locationHeader = httpMethod.getResponseHeader("Location");
                if (locationHeader == null)
                    throw new PluginImplementationException("Could not find download file location");
                httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(locationHeader.getValue())
                        .toGetMethod();
                break;
            } else if (PlugUtils.find(getDownloadLinkPageRegex(), getContentAsString())) { //page containing download link
                final Matcher downloadLinkMatcher = getMatcherAgainstContent(getDownloadLinkURLRegex());
                if (!downloadLinkMatcher.find()) {
                    throw new PluginImplementationException("Could not find download link URL");
                }
                final String downloadLink = downloadLinkMatcher.group(1);
                httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(downloadLink)
                        .toGetMethod();
                break;
            } else {
                if ((captchaExist && !isCaptchaExistInContent(getContentAsString())) || (!captchaExist)) {
                    checkDownloadProblems();
                    throw new PluginImplementationException("Download link not found");
                }
            }
            checkDownloadProblems();
            if (waitTimeCaptchaRetry()) doWaitTime();
        }
        setFileStreamContentTypes("text/plain");
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }


    protected HttpMethod stepReCaptcha(MethodBuilder methodBuilder) throws Exception {
        final Matcher reCaptchaKeyMatcher = getMatcherAgainstContent(getReCaptchaRegex());
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

    protected String stepFourTokensCaptcha() throws Exception {
        final Matcher captchaMatcher = getMatcherAgainstContent(getFourTokensCaptchaRegex());
        final StringBuilder strbuffCaptcha = new StringBuilder(4);
        final SortedMap<Integer, String> captchaMap = new TreeMap<Integer, String>();
        while (captchaMatcher.find()) {
            captchaMap.put(Integer.parseInt(captchaMatcher.group(1)), PlugUtils.unescapeHtml(captchaMatcher.group(2)));
        }
        for (String value : captchaMap.values()) {
            strbuffCaptcha.append(value);
        }
        final String strCaptcha;
        if (omitFourTokensCaptchaLeadingZero()) {
            strCaptcha = Integer.toString(Integer.parseInt(strbuffCaptcha.toString())); //omit leading '0'
        } else {
            strCaptcha = strbuffCaptcha.toString();
        }
        logger.info("Captcha : " + strCaptcha);

        return strCaptcha;
    }

    //'captchas' image captcha
    protected String stepCaptchas() throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final MethodBuilder methodBuilder = getMethodBuilder().setActionFromImgSrcWhereTagContains(getCaptchasImgTagContains());
        final String captchaURL = methodBuilder.getEscapedURI();
        logger.info("Captcha URL " + captchaURL);
        String captcha;
        if (captchaCounter <= captchaMax) {
            final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(captchaURL);
            captcha = new CaptchaRecognizer().recognize(captchaImage);
            logger.info("OCR attempt " + captchaCounter + " of " + captchaMax + ", recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaURL);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }
        return captcha;
    }

    //@TODO : captchaMax and captchaCounter
    protected HttpMethod stepCustomCaptcha(MethodBuilder methodBuilder) throws Exception {
        throw new PluginImplementationException("stepCustomCaptcha should be overrided");
    }

    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found") || contentAsString.contains("file was removed")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("server is in maintenance mode")) {
            throw new PluginImplementationException("This server is in maintenance mode. Please try again later.");
        }
    }

    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("till next download")) {
            String regexRule = "(?:(\\d+) hours?, )?(?:(\\d+) minutes?, )?(?:(\\d+) seconds?) till next download";
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
            throw new PluginImplementationException("Plugin is broken - Undefined subroutine");
        }
        if (contentAsString.contains("file reached max downloads limit")) {
            throw new PluginImplementationException("This file reached max downloads limit");
        }
        if (contentAsString.contains("You can download files up to")) {
            throw new PluginImplementationException(PlugUtils.getStringBetween(contentAsString, "<div class=\"err\">", "<br>"));
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
        if (contentAsString.contains("Wrong password")) {
            throw new PluginImplementationException("Wrong password");
        }
    }

}