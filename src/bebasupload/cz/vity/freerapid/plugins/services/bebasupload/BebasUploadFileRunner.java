package cz.vity.freerapid.plugins.services.bebasupload;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

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

    private boolean login() throws Exception {
        synchronized (BebasUploadFileRunner.class) {
            BebasUploadServiceImpl service = (BebasUploadServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();

            if (pa == null || !pa.isSet()) {
                logger.info("No account data set, skipping login");
                return false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction("http://bebasupload.com/")
                    .setParameter("op", "login")
                    .setParameter("redirect", "")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("submit", "")
                    .toPostMethod();
            addCookie(new Cookie(".bebasupload.com", "login", pa.getUsername(), "/", null, false));
            addCookie(new Cookie(".bebasupload.com", "xfss", "", "/", null, false));
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");
            if (getContentAsString().contains("Incorrect Login or Password"))
                throw new NotRecoverableDownloadException("Invalid BebasUpload registered account login information!");

            return true;
        }
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
        login();
        String content;

        GetMethod method = getGetMethod(fileURL); //create GET request
        if (!makeRedirectedRequest(method)) { //we make the main request
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        content = getContentAsString();

        HttpMethod httpMethod = getMethodBuilder()
                .setAction(fileURL)
                .setBaseURL(fileURL)
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("Free Download", true)
                .removeParameter("method_premium")
                .toHttpMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }

        content = getContentAsString();
        checkProblems();
        MethodBuilder methodBuilder = getMethodBuilder(content)
                .setActionFromFormByName("F1", true)
                .setAction(fileURL)
                .removeParameter("method_premium");

        //process wait time
        String waitTimeRule = "id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span";
        Matcher waitTimematcher = PlugUtils.matcher(waitTimeRule, content);
        if (waitTimematcher.find()) {
            downloadTask.sleep(Integer.parseInt(waitTimematcher.group(1)));
        }

        if (isPassworded()) {
            final String password = getDialogSupport().askForPassword("BebasUpload");
            if (password == null) {
                throw new NotRecoverableDownloadException("This file is secured with a password");
            }
            methodBuilder.setParameter("password", password);
        }

        if (content.contains("recaptcha")) {
            httpMethod = stepCaptcha(methodBuilder);
        } else {
            httpMethod = methodBuilder.toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
            }
            content = getContentAsString();
            checkProblems();
            
            client.getHTTPClient().getParams().setParameter(DownloadClientConsts.CONSIDER_AS_STREAM, "text/plain");
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains(httpFile.getFileName())
                    .toGetMethod();
            logger.info("Final URL : " + httpMethod.getURI().toString());

        }

        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }
    }

    private boolean isPassworded() {
        boolean passworded = getContentAsString().contains("<input type=\"password\" name=\"password\" class=\"myForm\">");
        return passworded;
    }

    private HttpMethod stepCaptcha(MethodBuilder methodBuilder) throws Exception {
        final Matcher reCaptchaKeyMatcher = getMatcherAgainstContent("recaptcha/api/noscript\\?k=(.*?)\"");
        if (!reCaptchaKeyMatcher.find()) {
            throw new PluginImplementationException("ReCaptcha key not found");
        }
        final String reCaptchaKey = reCaptchaKeyMatcher.group(1);

        logger.info("recaptcha public key : " + reCaptchaKey);

        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);
        return r.modifyResponseMethod(methodBuilder)
                .toPostMethod();
    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        int xMinutes = 0;
        int xSeconds = 0;
        int waittime;

        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        //Wrong captcha
        if (contentAsString.contains("Wrong captcha")) {
            throw new YouHaveToWaitException("Wrong captcha", 1); //let to know user in FRD
        }

        if (contentAsString.contains("This file reached max downloads limit")) {
            throw new ServiceConnectionProblemException("This file reached max downloads limit");
        }

        if (contentAsString.contains("You can download files up to")) {
            throw new NotRecoverableDownloadException("Need premium account for files bigger than 500 Mb"); //let to know user in FRD
        }


        if (contentAsString.contains("You have to wait")) {

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


