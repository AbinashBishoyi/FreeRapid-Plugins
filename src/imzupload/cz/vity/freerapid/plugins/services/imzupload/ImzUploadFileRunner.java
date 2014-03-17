package cz.vity.freerapid.plugins.services.imzupload;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class ImzUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ImzUploadFileRunner.class.getName());
    private int captchaCounter;


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h2>", "</h2>");
        PlugUtils.checkFileSize(httpFile, content, "</font> (", ")</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        captchaCounter = 0;
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormByIndex(1, true).setAction(fileURL).removeParameter("method_premium").toPostMethod();

            if (makeRedirectedRequest(httpMethod)) {
                checkProblems();

                final String repeatMessage = "Enter code below:";
                if (getContentAsString().contains(repeatMessage)) {
                    while (getContentAsString().contains(repeatMessage)) {
                        final int number = PlugUtils.getNumberBetween(getContentAsString(), "<span id=\"countdown\">", "</span>");
                        HttpMethod pmethod = stepCaptcha();

                        downloadTask.sleep(number);
                        if (!tryDownloadAndSaveFile(pmethod)) {//attempt to download file with captcha
                            checkProblems();//if downloading failed
                            if (getContentAsString().contains(repeatMessage)) {
                                if (!makeRedirectedRequest(httpMethod)) {//reload captcha page
                                    throw new ServiceConnectionProblemException(); //failed to load this page
                                } else continue;//step captcha again
                            }
                            logger.warning(getContentAsString());//log the info
                            throw new PluginImplementationException();//some unknown problem
                        }
                    }
                    //here is the download link extraction
                } else throw new PluginImplementationException("Enter code below not found");
            } else throw new PluginImplementationException("Main form not found");
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }

        Matcher content = getMatcherAgainstContent("wait ([0-9]+) minutes");
        if (content.find()) {
            throw new YouHaveToWaitException("You have reached the download-limit for free-users.", 60 * Integer.parseInt(content.group(1)));
        } else {
            content = getMatcherAgainstContent("Or wait ([0-9]+) minutes, ([0-9]+) seconds");
            if (content.find()) {
                throw new YouHaveToWaitException("You have reached the download-limit for free-users.", 60 * Integer.parseInt(content.group(1) + Integer.parseInt(content.group(2))));
            }
        }

        if (contentAsString.contains("You have reached the download-limit for free-users")) {
            throw new ServiceConnectionProblemException("You have reached the download-limit for free-users.");
        }
    }


    private HttpMethod stepCaptcha() throws Exception {

        String s = getMethodBuilder().setActionFromImgSrcWhereTagContains("captchas").getAction();
        logger.info("Captcha URL " + s);
        CaptchaSupport captchaSupport = getCaptchaSupport();

        final String captcha;
        if (captchaCounter < 4) {
            ++captchaCounter;
            final BufferedImage captchaImage = captchaSupport.getCaptchaImage(s);
            captcha = new CaptchaRecognizer().recognize(captchaImage);
        } else {
            captcha = captchaSupport.getCaptcha(s);
        }

        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            return getMethodBuilder().setReferer(fileURL).setActionFromFormByName("F1", true).setAction(fileURL).setParameter("code", captcha).toPostMethod();
        }


    }


}