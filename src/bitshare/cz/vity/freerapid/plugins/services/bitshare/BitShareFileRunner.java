package cz.vity.freerapid.plugins.services.bitshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Stan
 */
class BitShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BitShareFileRunner.class.getName());

    private static final String PARAM_REQUEST = "request";
    private static final String PARAM_AJAXID = "ajaxid";
    private static final String AJAX_HEADER_FIELD = "X-Requested-With";
    private static final String AJAX_HEADER_VALUE = "XMLHttpRequest";
    private static final String[] CONTENT_TYPE_TO = new String[]{"text/plain"};
    private static final String[] CONTENT_TYPE_FROM = new String[]{"application/json"};

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
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
    }

    @Override
    public void run() throws Exception {
        super.run();
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
            String captchaKey = PlugUtils.getStringBetween(content, "api.recaptcha.net/noscript?k=", "\"");
            HttpMethod captchaMethod = processCaptcha(captchaKey).modifyResponseMethod(getMethodBuilder(content)
                    .setAction(action).setParameter(PARAM_REQUEST, "validateCaptcha")
                    .setParameter(PARAM_AJAXID, ajaxdl).setReferer(fileURL)).toPostMethod();
            postMethodWithID.addRequestHeader(AJAX_HEADER_FIELD, AJAX_HEADER_VALUE); // send as AJAX
            setFileStreamContentTypes(CONTENT_TYPE_TO, CONTENT_TYPE_FROM); // JSON to plain text

            if (!makeRequest(captchaMethod) || !getContentAsString().equals("SUCCESS")) {
                checkProblems();
                throw new ServiceConnectionProblemException();
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

    private ReCaptcha processCaptcha(String key) throws Exception {

        final ReCaptcha reCaptcha = new ReCaptcha(key, client);
        final String captcha = getCaptchaSupport().getCaptcha(reCaptcha.getImageURL());
        if (captcha != null) {
            reCaptcha.setRecognized(captcha);
        }

        return reCaptcha;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("File not available")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("cant download more then 1 files at time")) {
            throw new YouHaveToWaitException("Cannot download more then 1 files at time", 60);
        }
        if (contentAsString.contains("SESSION ERROR")) {
            throw new ServiceConnectionProblemException("Connection problem");
        }
        if (contentAsString.contains("incorrect-captcha-sol")) {
            throw new CaptchaEntryInputMismatchException("Captcha recognition failed");
        }
    }

}