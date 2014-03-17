package cz.vity.freerapid.plugins.services.ifilez;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author RickCL
 * @since 0.85
 */
class IFilezFileRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(IFilezFileRunner.class.getName());
    private final static String SERVICE_BASE_URL = "http://depfile.com";
    private final static String SERVICE_COOKIE_DOMAIN = ".depfile.com";

    private void checkURL() {
        fileURL = fileURL.replaceFirst("i-filez\\.com", "depfile.com");
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL();
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "sdlanguageid", "2", "/", 86400, false));
        HttpMethod httpMethod = getMethodBuilder()
                .setAction(fileURL)
                .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        checkNameAndSize();
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkURL();
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "sdlanguageid", "2", "/", 86400, false));
        HttpMethod httpMethod = getMethodBuilder()
                .setAction(fileURL)
                .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        checkNameAndSize();

        while (getContentAsString().contains("verifycode")) {
            final MethodBuilder methodBuilder = getMethodBuilder()
                    .setBaseURL(SERVICE_BASE_URL)
                    .setActionFromFormWhereTagContains("verifycode", true)
                    .setParameter("verifycode", stepCaptcha());
            httpMethod = methodBuilder.toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
        }

        final String url = URLDecoder.decode(PlugUtils.getStringBetween(getContentAsString(), "document.getElementById(\"wait_input\").value= unescape('", "');"), "UTF-8");
        final int waitTime = PlugUtils.getWaitTimeBetween(getContentAsString(), "var sec=", ";", TimeUnit.SECONDS);
        downloadTask.sleep(waitTime);

        httpMethod = getMethodBuilder()
                .setAction(url)
                .setReferer(fileURL)
                .toGetMethod();
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String stepCaptcha() throws Exception {
        final MethodBuilder methodBuilder = getMethodBuilder().setBaseURL(SERVICE_BASE_URL).setActionFromImgSrcWhereTagContains("/vvc.php");
        final String captchaURL = methodBuilder.getEscapedURI();
        logger.info("Captcha URL " + captchaURL);
        final String captcha = getCaptchaSupport().getCaptcha(captchaURL);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        return captcha;
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException, UnsupportedEncodingException {
        String content = getContentAsString();
        String fileName = PlugUtils.getStringBetween(content, "<th>File name:</th>", "</td>").replaceAll("<[^>]*>", "").trim();
        String fileSize = PlugUtils.getStringBetween(content, "<th>Size:</th>", "</td>").replaceAll("<[^>]*>", "").trim();
        final long lsize = PlugUtils.getFileSizeFromString(fileSize);
        httpFile.setFileName(URLDecoder.decode(fileName, "UTF-8"));
        httpFile.setFileSize(lsize);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File was not found") || contentAsString.contains("0 byte")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("File is available only for Premium users")) {
            throw new PluginImplementationException("File is available only for Premium users");
        }
        if (contentAsString.contains("A file was recently downloaded from your IP address")) {
            final Matcher waitTimeMatcher = getMatcherAgainstContent("No less than (\\d+) min should");
            int waitTime = 5 * 60;
            if (waitTimeMatcher.find()) {
                waitTime = Integer.parseInt(waitTimeMatcher.group(1)) * 60;
            }
            throw new YouHaveToWaitException("A file was recently downloaded from your IP address", waitTime);
        }
    }

}
