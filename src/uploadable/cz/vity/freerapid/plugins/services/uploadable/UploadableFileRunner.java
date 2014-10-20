package cz.vity.freerapid.plugins.services.uploadable;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class UploadableFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadableFileRunner.class.getName());
    private final static String BaseURL = "http://www.uploadable.ch";

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        checkUrl();
        GetMethod getMethod = getGetMethod(fileURL);//make first request
        int status = client.makeRequest(getMethod,  false);
        if (status/100 == 3) {
            String redirect = getMethod.getResponseHeader("Location").getValue();
            if (!redirect.contains("http"))
                redirect = BaseURL + redirect;
            fileURL = redirect;
            status = client.makeRequest(getGetMethod(redirect), true);
            if (status != 200) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
        if (status == 200) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "file_name\" title=\"", "\"");
        PlugUtils.checkFileSize(httpFile, content, "filename_normal\">(", ")<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkUrl() {
        fileURL = fileURL.replaceFirst("http://(www\\.)?uploadable\\.ch", BaseURL);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkUrl();
        logger.info("Starting download in TASK " + fileURL);GetMethod getMethod = getGetMethod(fileURL);//make first request
        int status = client.makeRequest(getMethod,  false);
        if (status/100 == 3) {
            String redirect = getMethod.getResponseHeader("Location").getValue();
            if (!redirect.contains("http"))
                redirect = BaseURL + redirect;
            fileURL = redirect;
            status = client.makeRequest(getGetMethod(redirect), true);
            if (status != 200) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
        if (status == 200) {
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            downloadTask.sleep(getWaitTime() + 1);
            doJsonMethod("checkDownload", "check");
            if (!getContentAsString().contains("\"success\"")) {
                checkProblems();
                throw new PluginImplementationException("Processing Error 1");
            }
            final String response = PlugUtils.getStringBetween(getContentAsString(), "{\"success\":\"", "\"}");
            if (response.equals("showCaptcha")) {
                do {
                    final HttpMethod captchaMethod = doCaptcha(getMethodBuilder()
                            .setReferer(fileURL).setAction(BaseURL + "/checkReCaptcha.php")
                            .setAjax().setParameter("recaptcha_shortencode_field", PlugUtils.getStringBetween(contentAsString, "recaptcha_shortencode_field\" value=\"", "\"")
                            ), contentAsString).toPostMethod();
                    if (!makeRedirectedRequest(captchaMethod)) {
                        throw new ServiceConnectionProblemException();
                    }
                } while (!getContentAsString().contains("\"success\":1"));

                doJsonMethod("downloadLink", "show");
                if (getContentAsString().contains("fail"))
                    throw new PluginImplementationException("Error getting download link");
                if (getContentAsString().contains("forcePremiumDownload"))
                    throw new NotRecoverableDownloadException("Download as Premium");
            } else if (response.equals("showTimer"))
                throw new YouHaveToWaitException("Please wait", getWaitTime());

            final HttpMethod httpMethod = getMethodBuilder(contentAsString)
                    .setReferer(fileURL).setBaseURL(BaseURL)
                    .setActionFromFormByName("regularForm", true)
                    .toPostMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The file could not be found") ||
                contentAsString.contains("This file is no longer available") ||
                contentAsString.contains("File not available")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (getContentAsString().contains("fail\":\"timeLimit"))
            throw new YouHaveToWaitException("Please wait for 60 minutes to download the next file", 60 * 60);
        if (getContentAsString().contains("fail\":\"parallelDownload"))
            throw new YouHaveToWaitException("1 download at a time", 300);
    }

    private int getWaitTime() throws Exception {
        doJsonMethod("downloadLink", "wait");
        return PlugUtils.getNumberBetween(getContentAsString(), "{\"waitTime\":", "}");
    }

    private void doJsonMethod(final String paramName, final String paramValue) throws Exception {
        final HttpMethod jsonMethod = getMethodBuilder()
                .setReferer(fileURL).setAction(fileURL)
                .setAjax().setParameter(paramName, paramValue)
                .toPostMethod();
        if (!makeRedirectedRequest(jsonMethod)) {
            throw new ServiceConnectionProblemException();
        }
    }

    private MethodBuilder doCaptcha(final MethodBuilder builder, final String content) throws Exception {
        String key = PlugUtils.getStringBetween(content, "reCAPTCHA_publickey='", "'");
        final ReCaptcha reCaptcha = new ReCaptcha(key, client);
        final String captchaTxt = getCaptchaSupport().getCaptcha(reCaptcha.getImageURL());
        if (captchaTxt == null)
            throw new CaptchaEntryInputMismatchException();
        reCaptcha.setRecognized(captchaTxt);
        return reCaptcha.modifyResponseMethod(builder);
    }

}