package cz.vity.freerapid.plugins.services.bitshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Stan
 * @author RubinX
 */
class BitShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BitShareFileRunner.class.getName());

    private static final String PARAM_REQUEST = "request";
    private static final String PARAM_AJAXID = "ajaxid";

    private void setLanguageEN() {
        addCookie(new Cookie(".bitshare.com", "language_selection", "EN", "/", 86400, false));
    }

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        setLanguageEN();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("<h1>Downloading (.+?) - (.+?)</h1>", content);
        if (!match.find())
            throw new PluginImplementationException("File name/size not found");
        httpFile.setFileName(match.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(2).trim()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        final int i = fileURL.lastIndexOf('/');
        if (i > 0) {
            final int i2 = fileURL.toLowerCase(Locale.ENGLISH).lastIndexOf(".html");
            if (i2 > i) {
                httpFile.setFileName(fileURL.substring(i + 1, i2));
            }
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        setLanguageEN();
        logger.info("Starting download in TASK " + fileURL);

        final String content;

        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            content = getContentAsString();
            checkProblems();
            checkNameAndSize(content);//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        final String ajaxdl = PlugUtils.getStringBetween(content, "var ajaxdl = \"", "\";"); // download ID
        final String action = PlugUtils.getStringBetween(content, "http://bitshare.com", "request.html")
                + "request.html";

        final HttpMethod postMethodWithID = getMethodBuilder() // click to "Regular Download" button
                .setAjax().setAction(action).setParameter(PARAM_REQUEST, "generateID")
                .setParameter(PARAM_AJAXID, ajaxdl).setReferer(fileURL).toPostMethod();

        String[] typeTimeCaptcha;
        if (makeRequest(postMethodWithID)) {
            typeTimeCaptcha = getContentAsString().split(":");  // data in format "fileType:timeInSecond:captchaRequired"
            if (typeTimeCaptcha.length != 3 || !typeTimeCaptcha[1].matches("\\d+")) {
                throw new PluginImplementationException("Error parsing server response");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        final int waitTime = Integer.parseInt(typeTimeCaptcha[1]);
        if (waitTime > 300) {
            throw new YouHaveToWaitException("Wait time between downloads", waitTime + 5);
        }
        downloadTask.sleep(waitTime + 1);

        if ("1".equals(typeTimeCaptcha[2])) { // recognize captcha if is necessary
            while (!"SUCCESS".equals(getContentAsString())) {
                checkProblems();
                if (!makeRequest(stepCaptcha(content, action, ajaxdl))) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            }
        }

        final HttpMethod postMethodForUrl = getMethodBuilder() // click to "Download" button
                .setAjax().setAction(action).setParameter(PARAM_REQUEST, "getDownloadURL")
                .setParameter(PARAM_AJAXID, ajaxdl).setReferer(fileURL).toPostMethod();

        if (makeRequest(postMethodForUrl)) {
            final HttpMethod getMethodForDownload = getMethodBuilder()
                    .setAction(getContentAsString().substring("SUCCESS#".length()))
                    .setReferer(fileURL).toGetMethod();

            setFileStreamContentTypes("text/plain");
            if (!tryDownloadAndSaveFile(getMethodForDownload)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//set unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private HttpMethod stepCaptcha(String content, String action, String ajaxdl) throws Exception {

        String key = PlugUtils.getStringBetween(content, "api.recaptcha.net/noscript?k=", "\"");
        final ReCaptcha reCaptcha = new ReCaptcha(key, client);
        final String captcha = getCaptchaSupport().getCaptcha(reCaptcha.getImageURL());
        if (captcha != null) {
            reCaptcha.setRecognized(captcha);
        } else {
            throw new CaptchaEntryInputMismatchException();
        }
        return reCaptcha.modifyResponseMethod(getMethodBuilder(content)
                .setAction(action).setParameter(PARAM_REQUEST, "validateCaptcha")
                .setParameter(PARAM_AJAXID, ajaxdl).setReferer(fileURL)).toPostMethod();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("requested file was not found in our database")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("cant download more then 1 files at time")) {
            throw new YouHaveToWaitException("You cannot download more than one file at a time", 900); // 15 minutes
        }
        if (getContentAsString().contains("Traffic is used up for today")) {
            throw new YouHaveToWaitException("Your free traffic is used up for today", 7200); // 2 hours
        }
        if (getContentAsString().contains("Only Premium members can access this file")) {
            throw new NotRecoverableDownloadException("Only Premium members can access this file");
        }
    }
}