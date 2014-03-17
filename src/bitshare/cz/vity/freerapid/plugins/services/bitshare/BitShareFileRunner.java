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
    private static final String AJAX_HEADER_FIELD = "X-Requested-With";
    private static final String AJAX_HEADER_VALUE = "XMLHttpRequest";
    private static final String[] CONTENT_TYPE_TO = {"text/plain"};
    private static final String[] CONTENT_TYPE_FROM = {"application/json"};

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
        PlugUtils.checkName(httpFile, content, "<h1>Downloading ", " - ");
        PlugUtils.checkFileSize(httpFile, content, " - ", "</h1>");
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
                .setAction(action).setParameter(PARAM_REQUEST, "generateID")
                .setParameter(PARAM_AJAXID, ajaxdl).setReferer(fileURL).toPostMethod();
        postMethodWithID.addRequestHeader(AJAX_HEADER_FIELD, AJAX_HEADER_VALUE); // send as AJAX
        setFileStreamContentTypes(CONTENT_TYPE_TO, CONTENT_TYPE_FROM); // JSON to plain text

        String[] typeTimeCaptcha;
        if (makeRequest(postMethodWithID)) {
            typeTimeCaptcha = getContentAsString().split(":");  // data in format "fileType:timeInSecond:captchaRequired"
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        downloadTask.sleep(Integer.parseInt(typeTimeCaptcha[1])); // waiting

        if (Integer.parseInt(typeTimeCaptcha[2]) == 1) { // recognize captcha if is necessary
            while (!"SUCCESS".equals(getContentAsString())) {
                checkProblems();
                if (!makeRequest(stepCaptcha(content, action, ajaxdl))) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            }
        }

        final HttpMethod postMethodForUrl = getMethodBuilder() // click to "Download" button
                .setAction(action).setParameter(PARAM_REQUEST, "getDownloadURL")
                .setParameter(PARAM_AJAXID, ajaxdl).setReferer(fileURL).toPostMethod();
        postMethodWithID.addRequestHeader(AJAX_HEADER_FIELD, AJAX_HEADER_VALUE); // send as AJAX
        setFileStreamContentTypes(CONTENT_TYPE_TO, CONTENT_TYPE_FROM); // JSON to plain text

        if (makeRequest(postMethodForUrl)) {
            final HttpMethod getMethodForDownload = getMethodBuilder()
                    .setAction(getContentAsString().substring("SUCCESS#".length()))
                    .setReferer(fileURL).toGetMethod();

            //here is the download link extraction
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
        final HttpMethod postMethod = reCaptcha.modifyResponseMethod(getMethodBuilder(content)
                .setAction(action).setParameter(PARAM_REQUEST, "validateCaptcha")
                .setParameter(PARAM_AJAXID, ajaxdl).setReferer(fileURL)).toPostMethod();
        setFileStreamContentTypes(CONTENT_TYPE_TO, CONTENT_TYPE_FROM); // JSON to plain text

        return postMethod;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        Matcher matcher = PlugUtils.matcher("(?s)</h1>\\s*<div[^>]+class=\"quotbox2\"[^>]*>\\s*(.*?)\\s*</div>", getContentAsString());
        if (matcher.find()) {
            final String errorString = matcher.group(1).replaceFirst("(?s)\\s*<.*$", "");
            if (!errorString.isEmpty()) {
                if (errorString.contains("requested file was not found in our database"))
                    throw new URLNotAvailableAnymoreException(errorString); // permanent
                else if (errorString.contains("cant download more then 1 files at time"))
                    throw new YouHaveToWaitException(errorString, 900); // 15 minutes
                else if (errorString.contains("Traffic is used up for today"))
                    throw new YouHaveToWaitException(errorString, 21600); // 6 hours
                else // unknown reason
                    throw new ServiceConnectionProblemException(errorString); // default: 2 minutes
            }
        }
    }
}