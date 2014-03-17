package cz.vity.freerapid.plugins.services.filevelocity;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FileVelocityFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileVelocityFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkFileProblems();
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "Download File:</b> ", "</h2>");
        PlugUtils.checkFileSize(httpFile, content, "</font> (", ")</font></td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean login() throws Exception {
        synchronized (FileVelocityFileRunner.class) {
            FileVelocityServiceImpl service = (FileVelocityServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();

            //for testing purpose
            //pa.setPassword("freerapid");
            //pa.setUsername("freerapid");

            if (pa == null || !pa.isSet()) {
                logger.info("No account data set, skipping login");
                return false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction("http://filevelocity.com/login.html")
                    .setParameter("op", "login")
                    .setParameter("redirect", "")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("submit", "")
                    .toPostMethod();
            addCookie(new Cookie(".filevelocity.com", "login", pa.getUsername(), "/", null, false));
            addCookie(new Cookie(".filevelocity.com", "xfss", "", "/", null, false));
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");
            if (getContentAsString().contains("Incorrect Login or Password"))
                throw new NotRecoverableDownloadException("Invalid FileVelocity registered account login information!");

            return true;
        }
    }


    private boolean isPassworded() {
        boolean passworded = getContentAsString().contains("<input type=\"password\" name=\"password\" class=\"myForm\">");
        return passworded;
    }


    @Override
    public void run() throws Exception {
        super.run();

        login();

        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL); //create GET request
        if (!makeRedirectedRequest(method)) { //we make the main request
            logger.warning(getContentAsString());
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }

        checkFileProblems();//check problems
        checkNameAndSize(getContentAsString());//extract file name and size from the page
        
        HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setBaseURL(fileURL)
                .setActionFromFormWhereTagContains("Free Download", true)
                .removeParameter("method_premium")
                .toPostMethod();

        if (!makeRedirectedRequest(httpMethod)) {
            checkDownloadProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }

        checkDownloadProblems();

        final MethodBuilder methodBuilder = getMethodBuilder()
                .setActionFromFormByName("F1", true)
                .setAction(fileURL)
                .removeParameter("method_premium");

        //process wait time
        String waitTimeRule = "id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span";
        Matcher waitTimematcher = PlugUtils.matcher(waitTimeRule, getContentAsString());
        if (waitTimematcher.find()) {
            downloadTask.sleep(Integer.parseInt(waitTimematcher.group(1)));
        }

        if (isPassworded()) {
            final String password = getDialogSupport().askForPassword("FileVelocity");
            if (password == null) {
                throw new NotRecoverableDownloadException("This file is secured with a password");
            }
            methodBuilder.setParameter("password", password);
        }

        httpMethod = stepCaptcha(methodBuilder);

        //here is the download link extraction
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkDownloadProblems();//if downloading failed
            throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
        }
    }

    private HttpMethod stepCaptcha(MethodBuilder methodBuilder) throws Exception {
        //process captcha
        final Matcher reCaptchaKeyMatcher = getMatcherAgainstContent("recaptcha/api/challenge\\?k=(.*?)\">");
        reCaptchaKeyMatcher.find();
        final String reCaptchaKey = reCaptchaKeyMatcher.group(1);
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);

        return r.modifyResponseMethod(methodBuilder).toPostMethod();
    }


    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Wrong captcha")) {
            throw new YouHaveToWaitException("Wrong captcha", 1);
        }
        if (contentAsString.contains("seconds till next download")) {
            final int waitTime = PlugUtils.getWaitTimeBetween(contentAsString, "You have to wait ", " seconds till next download", TimeUnit.SECONDS);
            throw new YouHaveToWaitException("Wait between download", waitTime);
        }
        if (contentAsString.contains("Undefined subroutine")) {
            throw new PluginImplementationException("Server problem");
        }
    }

}