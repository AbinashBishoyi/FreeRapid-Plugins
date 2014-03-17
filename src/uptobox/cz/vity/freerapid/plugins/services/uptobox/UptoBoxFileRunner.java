package cz.vity.freerapid.plugins.services.uptobox;

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
class UptoBoxFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UptoBoxFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
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
        PlugUtils.checkName(httpFile, content, "<div class=\"page-top\">Download File", "</div>");
        PlugUtils.checkFileSize(httpFile, content, fileURL + " (", ")");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean login() throws Exception {
        synchronized (UptoBoxFileRunner.class) {
            UptoBoxServiceImpl service = (UptoBoxServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();

            //for testing purpose
            //pa.setPassword("freerapid");
            //pa.setUsername("freerapid");

            if (pa == null || !pa.isSet()) {
                logger.info("No account data set, skipping login");
                return false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction("http://uptobox.com/login.html")
                    .setParameter("op", "login")
                    .setParameter("redirect", "")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("submit", "")
                    .toPostMethod();
            addCookie(new Cookie(".uptobox.com", "login", pa.getUsername(), "/", null, false));
            addCookie(new Cookie(".uptobox.com", "xfss", "", "/", null, false));
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");
            if (getContentAsString().contains("Incorrect Login or Password"))
                throw new BadLoginException("Invalid UptoBox registered account login information!");

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
                .setBaseURL(fileURL)
                .setActionFromFormWhereTagContains("Free Download", true)
                .removeParameter("method_premium")
                .toPostMethod();

        if (!makeRedirectedRequest(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
        checkDownloadProblems();

        MethodBuilder methodBuilder = getMethodBuilder()
                .setActionFromFormByName("F1", true)
                .setAction(fileURL)
                .removeParameter("method_premium");

        String waitTimeRule = "id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span";
        Matcher waitTimematcher = PlugUtils.matcher(waitTimeRule, getContentAsString());
        if (waitTimematcher.find()) {
            downloadTask.sleep(Integer.parseInt(waitTimematcher.group(1)));
        }

        if (isPassworded()) {
            final String password = getDialogSupport().askForPassword("UptoBox");
            if (password == null) {
                throw new NotRecoverableDownloadException("This file is secured with a password");
            }
            methodBuilder.setParameter("password", password);
        }

        if (getContentAsString().contains("captcha_code")) {
            methodBuilder = stepCaptcha(methodBuilder);
        }

        if (!tryDownloadAndSaveFile(methodBuilder.toPostMethod())) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("seconds till next download")) {
            int xMinutes = 0;
            int xSeconds = 0;
            int waittime;

            if (contentAsString.contains("minute")) {
                Matcher matcher = PlugUtils.matcher("You have to wait ([0-9]+) minute(?:s)?, ([0-9]+) seconds", contentAsString);
                if (matcher.find()) {
                    xMinutes = new Integer(matcher.group(1));
                    xSeconds = new Integer(matcher.group(2));
                }
            } else {
                Matcher matcher = PlugUtils.matcher("You have to wait ([0-9]+) seconds", contentAsString);
                if (matcher.find()) xSeconds = new Integer(matcher.group(1));
            }
            waittime = xMinutes * 60 + xSeconds;
            throw new YouHaveToWaitException("You have to wait " + waittime + " seconds", waittime);
        }
        if (contentAsString.contains("Undefined subroutine")) {
            throw new PluginImplementationException("Server problem");
        }
        if (contentAsString.contains("This file reached max downloads limit")) {
            throw new PluginImplementationException("This file reached max downloads limit");
        }
    }

    private MethodBuilder stepCaptcha(MethodBuilder methodBuilder) throws Exception {
        logger.info("Processing captcha");
        final String contentAsString = getContentAsString();
        String captchaRule = "<span style='position:absolute;padding\\-left:(\\d+)px;padding\\-top:\\d+px;'>(\\d+)</span>";
        Matcher captchaMatcher = PlugUtils.matcher(captchaRule, PlugUtils.unescapeHtml(contentAsString));
        StringBuilder strbuffCaptcha = new StringBuilder(4);
        SortedMap<Integer, String> captchaMap = new TreeMap<Integer, String>();

        while (captchaMatcher.find()) {
            captchaMap.put(Integer.parseInt(captchaMatcher.group(1)), captchaMatcher.group(2));
        }
        for (String value : captchaMap.values()) {
            strbuffCaptcha.append(value);
        }
        String strCaptcha = Integer.toString(Integer.parseInt(strbuffCaptcha.toString())); //omitting leading '0'
        logger.info("Captcha : " + strCaptcha);

        return methodBuilder.setParameter("code", strCaptcha);
    }

}