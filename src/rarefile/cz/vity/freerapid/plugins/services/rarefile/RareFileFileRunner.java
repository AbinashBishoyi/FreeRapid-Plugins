package cz.vity.freerapid.plugins.services.rarefile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class RareFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RareFileFileRunner.class.getName());
    private static final String SERVICE_TITLE = "RareFile";
    private static final String SERVICE_COOKIE_DOMAIN = ".rarefile.net";
    private static final String SERVICE_LOGIN_ADDRESS = "http://www.rarefile.net/login.html";

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "english", "/", null, false));
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<td><font color=\"red\">", "</font></td>");
        PlugUtils.checkFileSize(httpFile, content, "<td>Size : ", "&nbsp;");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean login() throws Exception {
        synchronized (RareFileFileRunner.class) {
            RareFileServiceImpl service = (RareFileServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();

            //for testing purpose
            //pa.setPassword("freerapid");
            //pa.setUsername("freerapid");

            if (pa == null || !pa.isSet()) {
                logger.info("No account data set, skipping login");
                return false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction(SERVICE_LOGIN_ADDRESS)
                    .setParameter("op", "login")
                    .setParameter("redirect", "")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("submit", "")
                    .toPostMethod();
            addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "login", pa.getUsername(), "/", null, false));
            addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "xfss", "", "/", null, false));
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");
            if (getContentAsString().contains("Incorrect Login or Password"))
                throw new NotRecoverableDownloadException("Invalid " + SERVICE_TITLE + "registered account login information!");

            return true;
        }
    }

    private boolean isPassworded() {
        return getContentAsString().contains("<input type=\"password\" name=\"password\" class=\"myForm\">");
    }

    @Override
    public void run() throws Exception {
        super.run();
        login();
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "english", "/", null, false));
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkFileProblems();//check problems
            checkDownloadProblems();
            checkNameAndSize(contentAsString);//extract file name and size from the page

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
                final String password = getDialogSupport().askForPassword(SERVICE_TITLE);
                if (password == null) {
                    throw new NotRecoverableDownloadException("This file is secured with a password");
                }
                methodBuilder.setParameter("password", password);
            }

            if (getContentAsString().contains("Enter code below:")) {
                methodBuilder.setParameter("code", stepCaptcha(getContentAsString()));
            }

            final HttpMethod httpMethod = methodBuilder.toPostMethod();

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkFileProblems();//if downloading failed
                checkDownloadProblems();
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkFileProblems();
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String stepCaptcha(String content) {
        String captchaRule = "<span style=\\'position:absolute;padding\\-left:(\\d+)px;padding\\-top:\\d+px;\\'>(\\d+)</span>";
        Matcher captchaMatcher = PlugUtils.matcher(captchaRule, PlugUtils.unescapeHtml(content));
        StringBuffer strbuffCaptcha = new StringBuffer(4);
        SortedMap<Integer, String> captchaMap = new TreeMap<Integer, String>();

        while (captchaMatcher.find()) {
            captchaMap.put(Integer.parseInt(captchaMatcher.group(1)), captchaMatcher.group(2));
        }
        for (String value : captchaMap.values()) {
            strbuffCaptcha.append(value);
        }
        String strCaptcha = Integer.toString(Integer.parseInt(strbuffCaptcha.toString())); //ommit leading zero
        logger.info("Captcha : " + strCaptcha);
        return strCaptcha;
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("The file was removed by administrator")) {
            throw new URLNotAvailableAnymoreException("File was removed by administrator");
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("Wrong captcha")) {
            throw new YouHaveToWaitException("Wrong captcha", 1);
        }
        if (contentAsString.contains("This file reached max downloads limit")) {
            throw new ServiceConnectionProblemException("This file reached max downloads limit");
        }
        if (contentAsString.contains("You can download files up to")) {
            throw new NotRecoverableDownloadException("Need premium account for files bigger than 500 Mb"); //let to know user in FRD
        }
        if (contentAsString.contains("Undefined subroutine")) {
            throw new PluginImplementationException("Server problem");
        }
        if (contentAsString.contains("You have to wait")) {
            int xMinutes = 0;
            int xSeconds = 0;
            int waittime;
            if (contentAsString.contains("minute")) {
                Matcher matcher = PlugUtils.matcher("([0-9]+) minute(?:s)?, ([0-9]+) seconds", contentAsString);
                if (matcher.find()) {
                    xMinutes = new Integer(matcher.group(1));
                    xSeconds = new Integer(matcher.group(2));
                }
            } else {
                Matcher matcher = PlugUtils.matcher("([0-9]+) seconds", contentAsString);
                if (matcher.find()) xSeconds = new Integer(matcher.group(1));
            }
            waittime = xMinutes * 60 + xSeconds;
            throw new YouHaveToWaitException("You have to wait " + waittime + " seconds", waittime);

        }
    }

}