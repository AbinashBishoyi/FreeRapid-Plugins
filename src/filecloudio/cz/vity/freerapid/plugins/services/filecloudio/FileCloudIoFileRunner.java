package cz.vity.freerapid.plugins.services.filecloudio;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FileCloudIoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileCloudIoFileRunner.class.getName());
    private final static String SERVICE_COOKIE_DOMAIN = ".filecloud.io";

    @Override
    public void run() throws Exception {
        super.run();
        checkFileURL();
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "en", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        login();
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(httpMethod)) {
            checkFileProblems();
            final String downloadURL = httpMethod.getURI().toString();
            final String currentURL = getVar("__currentUrl");
            final String recaptchaKey = getVar("__recaptcha_public");
            final String requestUrl = getVar("__requestUrl");
            final String ukey = PlugUtils.getStringBetween(getContentAsString(), "'ukey'", ",").replace(":", "").replace("'", "").trim();
            final String ab1 = getVar("__ab1");
            httpMethod = getMethodBuilder()
                    .setReferer(currentURL)
                    .setAction(requestUrl)
                    .setParameter("ukey", ukey)
                    .setParameter("__ab1", ab1)
                    .toPostMethod();
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            setFileStreamContentTypes(new String[0], new String[]{"application/json"});
            if (makeRedirectedRequest(httpMethod)) {
                checkDownloadProblems();
                logger.info(getContentAsString());
                while (getContentAsString().contains("\"captcha\":1")) {
                    stepCaptcha(recaptchaKey, requestUrl, ukey, ab1);
                }
                checkDownloadProblems();
                httpMethod = getMethodBuilder()
                        .setReferer(downloadURL)
                        .setAction(downloadURL)
                        .toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkDownloadProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkDownloadProblems();
                httpMethod = getMethodBuilder()
                        .setReferer(downloadURL)
                        .setActionFromAHrefWhereATagContains("download")
                        .toGetMethod();
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkDownloadProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                checkDownloadProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        final String vError = getVar("__error");
        if (vError.equals("1")) {
            final String vErrorMsg = getVar("__error_msg").replace("l10n.", "");
            final String errorMsg = PlugUtils.getStringBetween(content, "\"" + vErrorMsg + "\":\"", "\"");
            if (errorMsg.contains("file removed") || errorMsg.contains("no such file") || errorMsg.contains("file expired")) {
                throw new URLNotAvailableAnymoreException("File not found");
            }
            if (errorMsg.contains("is currently rather busy") || errorMsg.contains("is currently busy")) {
                throw new YouHaveToWaitException("The server this file is on is currenty busy", 60);
            }
            if (errorMsg.contains("currently offline for maintenance") || errorMsg.contains("seems to be malfunctioning at the moment")) {
                throw new YouHaveToWaitException("The server this file is on is currently offline", 10 * 60);
            }
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("\"message\":\"signup\"")) {
            throw new PluginImplementationException("Signup for a free account in order to download this file");
        }
        if (contentAsString.contains("\"message\":\"gopremium\"")) {
            throw new PluginImplementationException("You need to have a premium account to download this file");
        }
    }

    /*
    // filename and size info is located at last page, so I don't think it's useful to check
    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher fileNameMatcher = getMatcherAgainstContent(
                "<span style=\"color: gray;\" id=\"aliasSpan\">\\s*([^<>]+?)\\s*</strong>".replace("&nbsp;", ""));
        if (!fileNameMatcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        final String fileName = fileNameMatcher.group(1).trim();
        if (fileName.equals("")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        final Matcher fileSizeMatcher = getMatcherAgainstContent("toMB\\(\\s*(\\d+)\\s*\\)");
        if (!fileSizeMatcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileName(fileName);
        httpFile.setFileSize(Integer.parseInt(fileSizeMatcher.group(1)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
    */

    private String getVar(final String name) throws ErrorDuringDownloadingException {
        final String regexp = "var\\s+" + Pattern.quote(name) + "\\s*=\\s*'?(.+?)'?;";
        final Matcher matcher = getMatcherAgainstContent(regexp);
        if (!matcher.find()) {
            throw new PluginImplementationException("Var '" + name + "' not found");
        }
        return matcher.group(1);
    }

    private void stepCaptcha(final String recaptchaKey, final String requestUrl, final String ukey, final String ab1) throws Exception {
        final ReCaptcha r = new ReCaptcha(recaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);
        final String captchaChallenge = PlugUtils.getStringBetween(r.getResponseParams(), "recaptcha_challenge_field=", "&recaptcha_response_field=");
        final MethodBuilder methodBuilder = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(requestUrl)
                .setParameter("ukey", ukey)
                .setParameter("__ab", ab1)
                .setParameter("ctype", "recaptcha")
                .setParameter("recaptcha_response", captcha)
                .setParameter("recaptcha_challenge", captchaChallenge);
        final HttpMethod httpMethod = methodBuilder.toPostMethod();
        httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
        if (!makeRedirectedRequest(httpMethod)) {
            throw new ServiceConnectionProblemException();
        }

    }

    private void checkFileURL() throws MalformedURLException {
        if (fileURL.matches("http://(?:www\\.)?ifile\\.it/.+")) {
            fileURL = fileURL.replaceFirst("ifile\\.it", "filecloud.io");
            httpFile.setNewURL(new URL(fileURL.replaceFirst("ifile\\.it", "filecloud.io")));
        }
    }

    private boolean login() throws Exception {
        synchronized (FileCloudIoFileRunner.class) {
            final FileCloudIoServiceImpl service = (FileCloudIoServiceImpl) getPluginService();
            final PremiumAccount pa = service.getConfig();
            if (pa == null || !pa.isSet()) {
                logger.info("No account data set, skipping login");
                return false;
            }
            HttpMethod httpMethod = getMethodBuilder()
                    .setAction("https://secure.filecloud.io/user-login.html")
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                throw new ServiceConnectionProblemException();
            }
            //sometimes ReCaptcha is shown during login
            do {
                final MethodBuilder methodBuilder = getMethodBuilder()
                        .setReferer("https://secure.filecloud.io/user-login.html")
                        .setActionFromFormWhereActionContains("user-login_p", true)
                        .setParameter("username", pa.getUsername())
                        .setParameter("password", pa.getPassword());
                if (getContentAsString().contains("recaptchaFld")) {
                    final String reCaptchaKey = getVar("__recaptcha_public");
                    final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
                    final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
                    if (captcha == null) {
                        throw new CaptchaEntryInputMismatchException();
                    }
                    r.setRecognized(captcha);
                    httpMethod = r.modifyResponseMethod(methodBuilder).toPostMethod();
                } else {
                    httpMethod = methodBuilder.toPostMethod();
                }
                makeRequest(httpMethod);
            } while (getContentAsString().contains("recaptchaFld"));
            if (getContentAsString().contains("password entered is too short") || getContentAsString().contains("check whether the username")) {
                logger.warning(getContentAsString());
                throw new BadLoginException("Invalid FileCloud account login information!");
            }
            return true;
        }
    }

}