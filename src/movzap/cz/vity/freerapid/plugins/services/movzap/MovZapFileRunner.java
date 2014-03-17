package cz.vity.freerapid.plugins.services.movzap;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.crypto.Cipher;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class MovZapFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MovZapFileRunner.class.getName());
    private final static String SERVICE_TITLE = "MovZap";
    private final static String SERVICE_COOKIE_DOMAIN = ".movzap.com";
    private final static String SERVICE_LOGIN_REFERER = "http://www.movzap.com/login.html";
    private final static String SERVICE_LOGIN_ACTION = "http://www.movzap.com";
    private final static byte[] SECRET_KEY = "N%66=]H6".getBytes(Charset.forName("UTF-8"));

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
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
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean login() throws Exception {
        synchronized (MovZapFileRunner.class) {
            MovZapServiceImpl service = (MovZapServiceImpl) getPluginService();
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

        final String downloadFormContent = getContentAsString();
        final String id = PlugUtils.getStringBetween(getContentAsString(), "\"id\" value=\"", "\"");
        HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("/xml/" + id + ".xml")
                .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
        checkDownloadProblems();

        httpFile.setFileName(PlugUtils.getStringBetween(getContentAsString(), "<title><![CDATA[", "]]></title>"));
        httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("/xml2/" + id + ".xml")
                .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
        checkDownloadProblems();

        String cipherDownloadURL;
        if (getContentAsString().contains("hd.file")) { //HD as default
            cipherDownloadURL = PlugUtils.getStringBetween(getContentAsString(), "<hd.file><![CDATA[", "]]></hd.file>");
        } else if (getContentAsString().contains("file")) {
            cipherDownloadURL = PlugUtils.getStringBetween(getContentAsString(), "<file><![CDATA[", "]]></file>");
        } else {
            throw new PluginImplementationException("Download link not found");
        }
        cipherDownloadURL = cipherDownloadURL.substring(0, cipherDownloadURL.length() - 6);
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(SECRET_KEY, "DES"));
        final String downloadURL = new String(cipher.doFinal(Base64.decodeBase64(cipherDownloadURL)));

        final String waitTimeRule = "id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span";
        final Matcher waitTimematcher = PlugUtils.matcher(waitTimeRule, downloadFormContent);
        if (waitTimematcher.find()) {
            downloadTask.sleep(Integer.parseInt(waitTimematcher.group(1)) + 1);
        } else {
            downloadTask.sleep(10);
        }

        httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(downloadURL)
                .setParameter("start", "0")
                .toGetMethod();

        setFileStreamContentTypes("text/plain");
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found") || contentAsString.contains("file was removed")) {
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
        if (contentAsString.contains("Wrong password")) {
            throw new PluginImplementationException("Wrong password");
        }
    }

}