package cz.vity.freerapid.plugins.services.nitroflare_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
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
 * @author birchie
 */
class NitroFlare_PremiumFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NitroFlare_PremiumFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "span title=\"", "\"");
        Matcher matcher = PlugUtils.matcher("File Size: </b><span[^<>]+?>([^<>]+?)</", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        long filesize = PlugUtils.getFileSizeFromString(matcher.group(1));
        logger.info("File size: " + filesize);
        httpFile.setFileSize(filesize);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();
            checkNameAndSize(getContentAsString());
            fileURL = method.getURI().toString(); //http redirected to https

            final Matcher mFile = PlugUtils.matcher("<input[^<>]+?name=\"fileId\"[^<>]+?value=\"([^\"]+?)\"", getContentAsString());
            final Matcher mPKey = PlugUtils.matcher("<input[^<>]+?name=\"password\"[^<>]+?value=\"([^\"]+?)\"", getContentAsString());
            if (!mFile.find()) throw new PluginImplementationException("File id not found");
            if (!mPKey.find()) throw new PluginImplementationException("Premium key not found");
            final String fileId = mFile.group(1);
            final String premiumKey = mPKey.group(1);
            final String timeStamp = "" + System.currentTimeMillis();
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL)
                    .setAction("/ajax/unlock.php")
                    .setParameter("password", premiumKey)
                    .setParameter("file", fileId)
                    .setParameter("keep", "false")
                    .setParameter("direct", "false")
                    .setParameter("_", timeStamp)
                    .setAjax()
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("download").toGetMethod();
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
        if (contentAsString.contains("File doesn't exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("You have to wait")) {
            Matcher matcher = PlugUtils.matcher("You have to wait (\\d+) minutes?", contentAsString);
            if (!matcher.find()) {
                throw new PluginImplementationException("Waiting time not found");
            }
            int waitingTime = Integer.parseInt(matcher.group(1).trim());
            throw new YouHaveToWaitException("You have to wait " + waitingTime + "minute(s) to download your next file", waitingTime * 60);
        }
    }

    private static Cookie userCookie;

    public void login() throws Exception {
        if (userCookie != null) {
            addCookie(userCookie);
            logger.info("LOGGED IN Using Cookie :)");
        } else {
            final NitroFlare_PremiumServiceImpl service = (NitroFlare_PremiumServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            synchronized (NitroFlare_PremiumServiceImpl.class) {
                if (!pa.isSet()) {
                    pa = service.showConfigDialog();
                    if (pa == null || !pa.isSet()) {
                        throw new BadLoginException("No NitroFlare Premium account login information!");
                    }
                }
            }
            do {
                if (!makeRedirectedRequest(getGetMethod("https://www.nitroflare.com/login"))) {
                    throw new ServiceConnectionProblemException("Error getting login page");
                }
                final MethodBuilder builder = getMethodBuilder()
                        .setActionFromFormWhereTagContains("login", true)
                        .setAction("https://www.nitroflare.com/login")
                        .setReferer("https://www.nitroflare.com/login")
                        .setParameter("email", pa.getUsername())
                        .setParameter("password", pa.getPassword())
                        .setParameter("login", "")
                        .setAjax();
                if (getContentAsString().contains("captcha")) {
                    final CaptchaSupport captchaSupport = getCaptchaSupport();
                    final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getEscapedURI();
                    final String captcha = captchaSupport.getCaptcha(captchaSrc);
                    if (captcha == null)
                        throw new CaptchaEntryInputMismatchException();
                    builder.setParameter("captcha", captcha);
                }
                if (!makeRedirectedRequest(builder.toPostMethod())) {
                    throw new ServiceConnectionProblemException("Error posting login info");
                }
            } while (getContentAsString().contains("CAPTCHA error"));

            if (getContentAsString().contains("Account does not exist") ||
                    getContentAsString().contains("Forgot your password") ||
                    getContentAsString().contains("Login failed")) {
                throw new BadLoginException("Invalid NitroFlare Premium account login information!");
            }
            userCookie = getCookieByName("user");
            logger.info("LOGGED IN :)");
        }
    }

}