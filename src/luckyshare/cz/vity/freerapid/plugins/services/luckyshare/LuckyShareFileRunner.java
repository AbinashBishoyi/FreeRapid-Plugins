package cz.vity.freerapid.plugins.services.luckyshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot, Abinash Bishoyi
 */
class LuckyShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LuckyShareFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "<h1 class='file_name'>", "</h1>");
        PlugUtils.checkFileSize(httpFile, content, "<span class='file_size'>Filesize: ", "</span></td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            String contentAsString = getContentAsString();//check for response

            //if this is placed in checkProblems(),it will catch YHTWExcept
            //but in this way we can still validate the link
            if (contentAsString.contains("<strong>Wait:</strong>")) {
                final int waitTime = PlugUtils.getWaitTimeBetween(contentAsString, "<span id=\"waitingtime\">", "</span>", TimeUnit.SECONDS);
                throw new YouHaveToWaitException("Waiting time between downloads", waitTime);
            }
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            final String reCaptchaKey = PlugUtils.getStringBetween(contentAsString, "Recaptcha.create(\"", "\",");
            final Matcher fileIdMatcher = PlugUtils.matcher("/(\\d+)/?", fileURL);
            fileIdMatcher.find();

            final String fileId = fileIdMatcher.group(1);

            HttpMethod getTimeMethod = getMethodBuilder()
                    .setAction("http://luckyshare.net/download/request/type/time/file/" + fileId)
                    .toGetMethod();
            getTimeMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");

            if (!makeRequest(getTimeMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            contentAsString = getContentAsString();
            logger.info("getTime : " + contentAsString);

            final String hash = PlugUtils.getStringBetween(contentAsString, "\"hash\":\"", "\",");
            final int time = PlugUtils.getWaitTimeBetween(contentAsString, "\"time\":", "}", TimeUnit.SECONDS);

            downloadTask.sleep(time);

            //captcha
            final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
            final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            }
            r.setRecognized(captcha);

            //there is no method to access "challenge" field directly,
            //get "challenge" field from response params instead..
            final String captchaChallenge = PlugUtils.getStringBetween(r.getResponseParams(), "recaptcha_challenge_field=", "&recaptcha_response_field=");
            final String captchaURL = "http://luckyshare.net/download/verify/challenge/" + captchaChallenge + "/response/" + captcha + "/hash/" + hash;
            logger.info("captcha   : " + captcha);
            logger.info("challenge : " + captchaChallenge);
            logger.info("hash      : " + hash);

            HttpMethod captchaMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(captchaURL)
                    .toGetMethod();
            captchaMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");

            if (!makeRequest(captchaMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            contentAsString = getContentAsString();
            logger.info("download link : " + contentAsString);

            final String downloadLink = PlugUtils.getStringBetween(contentAsString, "\"link\":\"", "\"}").replace("\\", "");
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadLink)
                    .toHttpMethod();

            //here is the download link extraction
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
        if (contentAsString.contains("There is no such file available")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        } /*else if (contentAsString.contains("<strong>Wait:</strong>")) {
            final int waitTime = PlugUtils.getWaitTimeBetween(contentAsString, "<span id=\"waitingtime\">", "</span>", TimeUnit.SECONDS);
            throw new YouHaveToWaitException("Waiting time between downloads", waitTime);
        }   */
        if (contentAsString.contains("This file is Premium only. Only Premium Users can download this file")) {
            throw new NotRecoverableDownloadException("Only Premium Users can download this file");
        }
    }

}