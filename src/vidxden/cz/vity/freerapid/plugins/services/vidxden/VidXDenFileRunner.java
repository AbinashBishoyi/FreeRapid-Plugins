package cz.vity.freerapid.plugins.services.vidxden;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class VidXDenFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(VidXDenFileRunner.class.getName());
    private final static String SERVICE_TITLE = "VidXDen";
    private final static String SERVICE_COOKIE_DOMAIN = ".vidxden.com";
    private final static String SERVICE_LOGIN_REFERER = "http://www.vidxden.com/login.html";
    private final static String SERVICE_LOGIN_ACTION = "http://www.vidxden.com";
    private final static String SERVICE_BASE_URL = "http://www.vidxden.com";

    private void checkURL() {
        fileURL = fileURL.replaceFirst("divxden\\.com", "vidxden.com");
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL();
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "english", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h2>Watch ", "</h2>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean login() throws Exception {
        synchronized (VidXDenFileRunner.class) {
            VidXDenServiceImpl service = (VidXDenServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();

            //for testing purpose
            //pa.setPassword("freerapid");
            //pa.setUsername("freerapid");
            if (pa == null || !pa.isSet()) {
                logger.info("No account data set, skipping login");
                return false;
            }
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(SERVICE_LOGIN_REFERER)
                    .setAction(SERVICE_LOGIN_ACTION)
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
                throw new BadLoginException("Invalid " + SERVICE_TITLE + " registered account login information!");
            return true;
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkURL();
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "english", "/", 86400, false));
        login();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        checkFileProblems();
        checkNameAndSize(getContentAsString());

        HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("method_free", true)
                .setAction(fileURL)
                .removeParameter("method_premium")
                .toPostMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
        checkDownloadProblems();

        final String waitTimeRule = "id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span";
        final Matcher waitTimematcher = PlugUtils.matcher(waitTimeRule, getContentAsString());
        if (waitTimematcher.find()) {
            downloadTask.sleep(Integer.parseInt(waitTimematcher.group(1)) + 1);
        } else {
            downloadTask.sleep(10);
        }

        final Matcher jsMatcher = getMatcherAgainstContent("<script type='text/javascript'>\\s*(" + Pattern.quote("eval(function(p,a,c,k,e,d)") + ".+?)\\s*</script>");
        if (!jsMatcher.find()) {
            throw new PluginImplementationException("Content generator javascript not found");
        }
        final String jsString = jsMatcher.group(1).replaceFirst(Pattern.quote("eval(function(p,a,c,k,e,d)"), "function test(p,a,c,k,e,d)")
                .replaceFirst(Pattern.quote("return p}"), "return p};test")
                .replaceFirst(Pattern.quote(".split('|')))"), ".split('|'));");
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        String downloadURL;
        try {
            final String jsEval = (String) engine.eval(jsString);
            if (jsEval.contains("DivXBrowserPlugin")) {
                downloadURL = PlugUtils.getStringBetween(jsEval, "\"src\"value=\"", "\"");
            } else if (jsEval.contains("SWFObject")) {
                downloadURL = PlugUtils.getStringBetween(jsEval, "'file','", "'");
            } else {
                throw new PluginImplementationException("Download link not found");
            }
        } catch (ScriptException e) {
            throw new PluginImplementationException("JavaScript eval failed");
        }

        httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(downloadURL)
                .toGetMethod();

        setFileStreamContentTypes("text/plain");
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found") || contentAsString.contains("file was removed") || contentAsString.contains("file has been removed")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("server is in maintenance mode")) {
            throw new PluginImplementationException("This server is in maintenance mode. Please try again later.");
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("till next download")) {
            String regexRule = "(?:([0-9]+) hours?, )?(?:([0-9]+) minutes?, )?(?:([0-9]+) seconds?) till next download";
            Matcher matcher = PlugUtils.matcher(regexRule, contentAsString);
            int waitHours = 0, waitMinutes = 0, waitSeconds = 0, waitTime;
            if (matcher.find()) {
                if (matcher.group(1) != null)
                    waitHours = Integer.parseInt(matcher.group(1));
                if (matcher.group(2) != null)
                    waitMinutes = Integer.parseInt(matcher.group(2));
                waitSeconds = Integer.parseInt(matcher.group(3));
            }
            waitTime = (waitHours * 60 * 60) + (waitMinutes * 60) + waitSeconds;
            throw new YouHaveToWaitException("You have to wait " + waitTime + " seconds", waitTime);
        }
        if (contentAsString.contains("Undefined subroutine")) {
            throw new PluginImplementationException("Plugin is broken - Undefined subroutine");
        }
        if (contentAsString.contains("file reached max downloads limit")) {
            throw new PluginImplementationException("This file reached max downloads limit");
        }
        if (contentAsString.contains("You can download files up to")) {
            throw new PluginImplementationException(PlugUtils.getStringBetween(contentAsString, "<div class=\"err\">", "<br>"));
        }
        if (contentAsString.contains("have reached the download-limit")) {
            throw new YouHaveToWaitException("You have reached the download-limit", 10 * 60);
        }
        if (contentAsString.contains("Error happened when generating Download Link")) {
            throw new YouHaveToWaitException("Error happened when generating Download Link", 60);
        }
        if (contentAsString.contains("file is available to premium users only")) {
            throw new PluginImplementationException("This file is available to premium users only");
        }
        if (contentAsString.contains("this file requires premium to download")) {
            throw new PluginImplementationException("This file is available to premium users only");
        }
    }

}