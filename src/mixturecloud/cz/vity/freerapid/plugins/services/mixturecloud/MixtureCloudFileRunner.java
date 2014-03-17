package cz.vity.freerapid.plugins.services.mixturecloud;

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
 * @author tong2shot
 */
class MixtureCloudFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MixtureCloudFileRunner.class.getName());
    private final static String SERVICE_COOKIE_DOMAIN = ".mixturecloud.com";

    private void checkURL() {
        if (fileURL.contains("/video=")) fileURL = fileURL.replaceFirst("/video=", "/media/download/");
        if (fileURL.contains("/download=")) fileURL = fileURL.replaceFirst("/download=", "/media/download/");
        if ((fileURL.contains("/media/")) && (!fileURL.contains("/media/download/")))
            fileURL = fileURL.replaceFirst("/media/", "/media/download/");
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setClientParameter(DownloadClientConsts.USER_AGENT, "Opera/9.80 (Windows NT 5.1; U; en) Presto/2.10.289 Version/12.00");
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "en", "/", 86400, false));
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "mx_l", "en", "/", 86400, false));
        checkURL();
        logger.info(fileURL);
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
        final String fileName;
        Matcher matcher = getMatcherAgainstContent("<!--\\s*File header informations\\s*-->\\s*<h1>(.+?)</h1>");
        if (!matcher.find()) throw new PluginImplementationException("Filename not found");
        fileName = matcher.group(1).trim();
        httpFile.setFileName(fileName);
        PlugUtils.checkFileSize(httpFile, content, "<h5>Size :", "</h5>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        setClientParameter(DownloadClientConsts.USER_AGENT, "Opera/9.80 (Windows NT 5.1; U; en) Presto/2.10.289 Version/12.00");
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "en", "/", 86400, false));
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "mx_l", "en", "/", 86400, false));
        checkURL();
        logger.info("Starting download in TASK " + fileURL);
        login();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());
            checkDownloadProblems();

            HttpMethod httpMethod;
            while (getContentAsString().contains("recaptcha/api/challenge")) {
                final MethodBuilder methodBuilder = getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromFormByName("reCaptcha", true)
                        .setAction(fileURL);
                httpMethod = stepCaptcha(methodBuilder);
                if (!makeRedirectedRequest(httpMethod)) {
                    checkDownloadProblems();
                    throw new PluginImplementationException();
                }
            }
            checkDownloadProblems();
            Matcher matcher = getMatcherAgainstContent("var time\\s*=\\s*(\\d+)");
            if (!matcher.find()) {
                throw new PluginImplementationException("Wait time not found");
            }
            final int waitTime = Integer.parseInt(matcher.group(1));
            matcher = getMatcherAgainstContent("<a .*?href\\s*=\\s*[\"'](http://.+?)[\"'].*?>\\s*Download free\\s*</a>");
            if (!matcher.find()) {
                throw new PluginImplementationException("Download link not found");
            }

            final String downloadURL = matcher.group(1);
            downloadTask.sleep(waitTime + 1);
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadURL)
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkDownloadProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private HttpMethod stepCaptcha(MethodBuilder methodBuilder) throws Exception {
        final Matcher reCaptchaKeyMatcher = getMatcherAgainstContent("recaptcha/api/challenge\\?k=(.*?)\">");
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

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<h3>You have to wait")) {
            throw new YouHaveToWaitException("You have to wait 1200 seconds to start a new download", 1201);
        }
        if (contentAsString.contains("You have already downloaded")) {
            throw new YouHaveToWaitException("You have already downloaded a file over 10 MB in the last 30 minutes", 31 * 60);
        }
        if (getContentAsString().contains("access is limited to users")) {
            throw new PluginImplementationException("File access is limited to users with unlimited or free account");
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("we removed the file") || contentAsString.contains("No videos at this address") || contentAsString.contains("In response to a complaint")) {
            throw new URLNotAvailableAnymoreException("File was removed");
        }
    }

    private boolean login() throws Exception {
        synchronized (MixtureCloudFileRunner.class) {
            final MixtureCloudServiceImpl service = (MixtureCloudServiceImpl) getPluginService();
            final PremiumAccount pa = service.getConfig();
            if (pa == null || !pa.isSet()) {
                logger.info("No account data set, skipping login");
                return false;
            }
            HttpMethod httpMethod = getMethodBuilder()
                    .setAction("http://www.mixturecloud.com/login")
                    .setParameter("back", "http://www.mixturecloud.com/price")
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                throw new ServiceConnectionProblemException("Error retrieving login page");
            }
            //sometimes ReCaptcha is shown during login
            do {
                final MethodBuilder methodBuilder = getMethodBuilder()
                        .setReferer("http://www.mixturecloud.com/login")
                        .setActionFromFormWhereActionContains("login", true)
                        .setAction("http://www.mixturecloud.com/login")
                        .setParameter("email", pa.getUsername())
                        .setParameter("password", pa.getPassword())
                        .setParameter("back", "http://www.mixturecloud.com/price")
                        .setParameter("submit", "Login");
                if (getContentAsString().contains("recaptcha/api/challenge")) {
                    httpMethod = stepCaptcha(methodBuilder);
                } else {
                    httpMethod = methodBuilder.toPostMethod();
                }
                makeRequest(httpMethod);
            } while (getContentAsString().contains("recaptcha/api/challenge"));
            if (getContentAsString().contains("Your email or password are not valid")) {
                logger.warning(getContentAsString());
                throw new BadLoginException("Invalid MixtureCloud account login information!");
            }
            return true;
        }
    }

}