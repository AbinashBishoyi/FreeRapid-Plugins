package cz.vity.freerapid.plugins.services.filesmonster;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.filesmonster.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Lukiz, ntoskrnl
 */
class FilesMonsterFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilesMonsterFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "File name: <span class=\"em\">", "</span>");
        PlugUtils.checkFileSize(httpFile, content, "File size: <span class=\"em\">", "</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        if (!content.contains("slowdownload"))
            throw new NotRecoverableDownloadException("Only Premium download available");
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("slowdownload", true).toPostMethod();
            final String refer = "http://filesmonster.com" + httpMethod.getPath();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            while (getContentAsString().contains("recaptcha")) {
                if (!makeRedirectedRequest(stepCaptcha(refer))) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            }

            downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "var timeout='", "';") + 1);

            final HttpMethod ticketMethod = getMethodBuilder().setReferer(refer).setActionFromFormByName("rtForm", true).setAction("http://filesmonster.com/ajax.php").toHttpMethod();
            ticketMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");//use AJAX
            if (!makeRedirectedRequest(ticketMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkJSONProblems(getContentAsString());

            String data = PlugUtils.getStringBetween(getContentAsString(), "\"text\":\"", "\",\"");
            final HttpMethod getUrlMethod = getMethodBuilder().setReferer(refer).setAction("http://filesmonster.com/ajax.php").setParameter("act", "getdl").setParameter("data", data).toPostMethod();
            getUrlMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");//use AJAX
            if (!makeRedirectedRequest(getUrlMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkJSONProblems(getContentAsString());

            String finalUrl = PlugUtils.getStringBetween(getContentAsString(), "\"url\":\"", "\",\"file_request");
            String request = PlugUtils.getStringBetween(getContentAsString(), "file_request\":\"", "\",\"err");
            finalUrl = finalUrl.replace("\\", "");

            final HttpMethod finalMethod = getMethodBuilder().setReferer(refer).setAction(finalUrl).setParameter("X-File-Request", request).toPostMethod();
            finalMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");//use AJAX
            //here is the download link extraction
            if (!tryDownloadAndSaveFile(finalMethod)) {
                checkJSONProblems(getContentAsString());
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("File was deleted by owner") || contentAsString.contains("This document does not exist"))
            throw new URLNotAvailableAnymoreException("File not found");

        if (contentAsString.contains("You can wait for the start of downloading"))
            throw new YouHaveToWaitException("You have got max allowed download sessions from the same IP", PlugUtils.getWaitTimeBetween(contentAsString, " start of downloading", " minute", TimeUnit.MINUTES));

        if (contentAsString.contains("There are no free download slots available"))
            throw new ServiceConnectionProblemException("No more free download slots");
    }

    private void checkJSONProblems(final String content) throws ErrorDuringDownloadingException {
        final String error;
        try {
            error = PlugUtils.getStringBetween(content, "\"error\":\"", "\"");
        } catch (PluginImplementationException e) {
            return;//no error
        }
        if (error == null || error.isEmpty()) return;

        if (error.contains("404"))
            throw new ServiceConnectionProblemException("AJAX server response: There is no such file");
        if (error.contains("500"))
            throw new ServiceConnectionProblemException("AJAX server response: Internal error");
        //else
        throw new ServiceConnectionProblemException("AJAX server response: No free download slots available");
    }

    private HttpMethod stepCaptcha(final String referer) throws Exception {
        final Matcher m = getMatcherAgainstContent("api.recaptcha.net/noscript\\?k=([^\"]+)\"");
        if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");
        final String reCaptchaKey = m.group(1);

        final String content = getContentAsString();
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final CaptchaSupport captchaSupport = getCaptchaSupport();

        final String captchaURL = r.getImageURL();
        logger.info("Captcha URL " + captchaURL);

        final String captcha = captchaSupport.getCaptcha(captchaURL);
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        r.setRecognized(captcha);

        return r.modifyResponseMethod(getMethodBuilder(content).setReferer(referer).setActionFromFormWhereTagContains("recaptcha", true).setAction("http://filesmonster.com/get/free/")).toPostMethod();
    }

}