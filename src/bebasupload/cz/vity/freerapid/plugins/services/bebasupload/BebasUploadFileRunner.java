package cz.vity.freerapid.plugins.services.bebasupload;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Arthur Gunawan
 */
class BebasUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BebasUploadFileRunner.class.getName());


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

    private void Login() throws Exception {
        PostMethod postMethod = getPostMethod("http://bebasupload.com/");

        postMethod.addParameter("op", "login");
        postMethod.addParameter("redirect", "");
        postMethod.addParameter("login", "freerapid");
        postMethod.addParameter("password", "freerapid");
        postMethod.addParameter("submit", "");


        addCookie(new Cookie(".bebasupload.com", "login", "freerapid", "/", null, false));
        addCookie(new Cookie(".bebasupload.com", "xfss", "", "/", null, false));

        makeRedirectedRequest(postMethod);


    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h2>Download File ", "</h2>");//TODO
        PlugUtils.checkFileSize(httpFile, content, "</font> (", ")</font>");//TODO
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();


        logger.info("Starting download in TASK " + fileURL);
        Login();
        String content = getContentAsString();


        GetMethod method = getGetMethod(fileURL); //create GET request
        if (!makeRedirectedRequest(method)) { //we make the main request
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        content = getContentAsString();


        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).setBaseURL(fileURL).setReferer(fileURL).setActionFromFormWhereTagContains("Free Download", true).removeParameter("method_premium").toHttpMethod();//TODO
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }
        content = getContentAsString();

        checkProblems();

        if (content.contains("Wait <span id=\"countdown\">")) {
            String stringWait = PlugUtils.getStringBetween(content, "Wait <span id=\"countdown\">", "</span>");
            int wait = new Integer(stringWait);
            downloadTask.sleep(wait);
        }
        if (content.contains("captcha")) {
            logger.info("CAPTCHA INPUT!!!");

            httpMethod = stepCaptcha();

        } else {
            httpMethod = getMethodBuilder().setAction(fileURL).setBaseURL(fileURL).setReferer(fileURL).setActionFromFormByName("F1", true).removeParameter("method_premium").toHttpMethod();//TODO

        }

        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }
        content = getContentAsString();
        logger.info(content);
        checkProblems();

    }

    private HttpMethod stepCaptcha() throws Exception {

        CaptchaSupport captchaSupport = getCaptchaSupport();
        String s = getMethodBuilder().setActionFromImgSrcWhereTagContains("captchas").getAction();
        checkProblems();


        logger.info("Captcha URL " + s);
        String captcha = captchaSupport.getCaptcha(s);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            final HttpMethod postMethod = getMethodBuilder().setActionFromFormByName("F1", true).setAction(fileURL).
                    setParameter("code", captcha).removeParameter("method_premium").toPostMethod();
            return postMethod;

        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        int xMinutes = 0;
        int xSeconds = 0;
        int waittime;

        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        //Wrong captcha
        if (contentAsString.contains("Wrong captcha")) {//TODO
            throw new YouHaveToWaitException("Wrong captcha", 1); //let to know user in FRD
        }

        if (contentAsString.contains("You can download files up to")) {//TODO
            throw new NotRecoverableDownloadException("Need premium account for files bigger than 500 Mb"); //let to know user in FRD
        }


        if (contentAsString.contains("You have to wait")) {//TODO

            if (contentAsString.contains("minute")) {
                logger.info("Minutes WAIT!!!");

                Matcher matcher = PlugUtils.matcher("You have to wait ([0-9]+) minute(s)?, ([0-9]+) seconds", contentAsString);
                if (matcher.find()) {
                    xMinutes = new Integer(matcher.group(1));
                    xSeconds = new Integer(matcher.group(2));
                }
            } else {
                Matcher matcher = PlugUtils.matcher("You have to wait ([0-9]+) seconds", contentAsString);
                if (matcher.find()) xSeconds = new Integer(matcher.group(1));
            }
            waittime = xMinutes * 60 + xSeconds;
            throw new YouHaveToWaitException("You have to wait " + waittime + " seconds", waittime); //let to know user in FRD

        }
    }
}


