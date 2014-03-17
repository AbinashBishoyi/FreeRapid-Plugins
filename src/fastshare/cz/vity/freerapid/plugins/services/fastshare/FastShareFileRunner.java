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

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FastShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FastShareFileRunner.class.getName());
    private final static String SERVICE_COOKIE_DOMAIN = ".fastshare.cz";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "cs", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<span style=color:black;>", "</b>");
        PlugUtils.checkFileSize(httpFile, content, "Velikost: </td><td style=font-weight:bold>", "</td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "cs", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            final String formAction = PlugUtils.getStringBetween(getContentAsString(), "action=", "><b>Stáhnout FREE");
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                            //.setActionFromFormByIndex(2, true)
                            //.setActionFromFormWhereTagContains("Stáhnout FREE", true)
                    .setAction(formAction)
                    .setParameter("code", stepCaptcha())
                    .setParameter("submit", "stáhnout")
                    .toPostMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                if (getContentAsString().contains("Opište kód")) {
                    throw new YouHaveToWaitException("Wrong captcha", 8);
                }
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String stepCaptcha() throws Exception {
        CaptchaSupport captchaSupport = getCaptchaSupport();
        String captchaURL = "http://www.fastshare.cz" + PlugUtils.getStringBetween(getContentAsString(), "Opište kód:<br><img src=\"", "\"><br>");
        logger.info("Captcha URL " + captchaURL);
        String captcha = captchaSupport.getCaptcha(captchaURL);
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
        if (contentAsString.contains("FREE slotů je plných")) {
            throw new YouHaveToWaitException("FREE slots are full", 60);
        }
    }

}