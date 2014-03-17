package cz.vity.freerapid.plugins.services.xfilesharing;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.awt.image.BufferedImage;
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
public class XFileSharingRunner extends AbstractRunner {
    protected final static Logger logger = Logger.getLogger(XFileSharingRunner.class.getName());
    private final int captchaMax = 8; //used in stepCaptchas()
    private int captchaCounter = 0; //used in stepCaptchas()
    protected String cookieDomain; //ex : ".ryushare.com"
    protected String serviceTitle; //ex : "RyuShare"

    protected RegisteredUser registeredUser;
    protected CustomCaptcha customCaptcha;
    protected CustomRun customRun;

    public XFileSharingRunner() {
        super();
    }

    public XFileSharingRunner(String cookieDomain, String serviceTitle) {
        super();
        this.cookieDomain = cookieDomain;
        this.serviceTitle = serviceTitle;
    }

    protected void checkPrerequisites() throws PluginImplementationException {
        if (cookieDomain == null)
            throw new PluginImplementationException("cookieDomain cannot be null.");
        if (serviceTitle == null)
            throw new PluginImplementationException("serviceTitle cannot be null.");
        if ((getNumberOfPages() < 1) || (getNumberOfPages() > 2))
            throw new PluginImplementationException("Number of pages should be 1 or 2.");
        if ((customCaptcha != null) && (customCaptcha.getCustomCaptchaRegex() == null))
            throw new PluginImplementationException("getCustomCaptchaRegex cannot be null");
    }

    //should be overrided.
    //if filename and size doesn't need to be checked, simply type : httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    protected void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        throw new PluginImplementationException("checkNameAndSize should be overrided");
    }

    //return value should be 1 or 2
    //mostly there are 2 pages that contains 'method_free' in FORM tag,
    //but some sites only show 1 page that contains 'method_free' in FORM tag
    protected int getNumberOfPages() {
        return 2;
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
        if (customCaptcha != null) captchaRegex = captchaRegex + "|" + customCaptcha.getCustomCaptchaRegex();
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

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkPrerequisites();
        addCookie(new Cookie(cookieDomain, "lang", "english", "/", 86400, false));
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
        addCookie(new Cookie(cookieDomain, "lang", "english", "/", 86400, false));
        if (registeredUser != null) registeredUser.login();
        if (customRun != null) {
            customRun.customRun();
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
                    .setActionFromFormWhereTagContains("method_free", true) //@TODO : I haven't yet found site that doesn't use 'method_free'. Should the value be able to be customized ?
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
            password = getDialogSupport().askForPassword(serviceTitle);
            if (password == null) {
                throw new NotRecoverableDownloadException("This file is secured with a password");
            }
        }
        final boolean captchaExist = isCaptchaExistInContent(getContentAsString());

        while (true) {
            MethodBuilder methodBuilder = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("method_free", true)  //@TODO : I haven't yet found site that doesn't use 'method_free'. Should the value be able to be customized ?
                    .setAction(fileURL)
                    .removeParameter("method_premium");
            if (isPassworded(getContentAsString())) {
                methodBuilder.setParameter("password", password);
            }
            if (PlugUtils.find(getReCaptchaRegex(), getContentAsString())) {
                httpMethod = stepReCaptcha(methodBuilder);
            } else if (PlugUtils.find(getFourTokensCaptchaRegex(), getContentAsString())) {
                httpMethod = stepFourTokensCaptcha(methodBuilder);
            } else if (getContentAsString().contains(getCaptchasImgTagContains())) {
                httpMethod = stepCaptchas(methodBuilder);
            } else if ((customCaptcha != null) && PlugUtils.find(customCaptcha.getCustomCaptchaRegex(), getContentAsString())) {
                httpMethod = customCaptcha.stepCustomCaptcha(methodBuilder);
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

    protected HttpMethod stepFourTokensCaptcha(MethodBuilder methodBuilder) throws Exception {
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
        methodBuilder.setParameter("code", strCaptcha);

        return methodBuilder.toPostMethod();
    }

    //'captchas' image captcha
    protected HttpMethod stepCaptchas(MethodBuilder methodBuilder) throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final MethodBuilder methodBuilder2 = getMethodBuilder().setActionFromImgSrcWhereTagContains(getCaptchasImgTagContains());
        final String captchaURL = methodBuilder2.getEscapedURI();
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
        methodBuilder.setParameter("code", captcha);
        return methodBuilder.toPostMethod();
    }

    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found") || contentAsString.contains("file was removed") || contentAsString.contains("file has been removed")) {
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