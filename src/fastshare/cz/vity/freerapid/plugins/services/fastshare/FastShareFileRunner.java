package cz.vity.freerapid.plugins.services.fastshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot, birchie
 */
class FastShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FastShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final String SERVICE_COOKIE_DOMAIN = "." + httpFile.getFileUrl().getAuthority().replaceFirst("www.", "");
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "cs", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);
        //site returning status code 404 when page loads correctly ??
        makeRedirectedRequest(getMethod);
        checkProblems();
        checkNameAndSize(getContentAsString());
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h1 class=\"dwp\">", "</h1>");
        try {
            PlugUtils.checkFileSize(httpFile, content, "Velikost:", ", ");
        } catch (Exception e) {
            PlugUtils.checkFileSize(httpFile, content, ": ", ", File type");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        final String SERVICE_COOKIE_DOMAIN = "." + httpFile.getFileUrl().getAuthority().replaceFirst("www.", "");
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "cs", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        //site returning status code 404 when page loads correctly ??
        makeRedirectedRequest(method);
        final String contentAsString = getContentAsString();
        checkDownloadProblems();
        checkNameAndSize(contentAsString);
        final Matcher match = PlugUtils.matcher("<form.+?action=(/free[^>]+?)>", getContentAsString());
        if (!match.find())
            throw new PluginImplementationException("Download form not found");
        HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(match.group(1))
                .setParameter("code", stepCaptcha())
                .toPostMethod();

        if (!tryDownloadAndSaveFile(httpMethod)) {
            if (getContentAsString().contains("Opište kód")) {
                throw new YouHaveToWaitException("Wrong captcha", 8);
            }
            checkDownloadProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private String stepCaptcha() throws Exception {
        CaptchaSupport captchaSupport = getCaptchaSupport();
        final Matcher match = PlugUtils.matcher("<img src=\"(/securimage.+?)\">", getContentAsString());
        if (!match.find())
            throw new PluginImplementationException("Captcha image not found");
        final String baseUrl = httpFile.getFileUrl().getProtocol() + "://" + httpFile.getFileUrl().getAuthority();
        final String captchaURL = baseUrl + match.group(1);
        logger.info("Captcha URL " + captchaURL);
        final String captcha = captchaSupport.getCaptcha(captchaURL);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        return captcha;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<title>FastShare.cz</title>") && !contentAsString.contains("Stáhnout FREE")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("Tento soubor byl smazán")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        checkProblems();
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("muzete stahovat jen jeden soubor najednou")) {
            throw new ErrorDuringDownloadingException("You can download only one file at a time");
        }
        if (contentAsString.contains("FREE slotů je plných")) {
            throw new YouHaveToWaitException("FREE slots are full", 60);
        }
    }

}