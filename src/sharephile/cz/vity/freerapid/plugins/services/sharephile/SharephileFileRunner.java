package cz.vity.freerapid.plugins.services.sharephile;

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
 * @author JPEXS
 */
class SharephileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SharephileFileRunner.class.getName());
    private static final String SERVER_URL = "http://sharephile.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".sharephile.com", "user_lang", "en", "/", 86400, false));
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
        String namePart = PlugUtils.getStringBetween(content, "<span class='file-icon1", "</span>") + "</span>";
        PlugUtils.checkName(httpFile, namePart, "'>", "</span>");
        PlugUtils.checkFileSize(httpFile, content, "</span>\t\t(", ")");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(".sharephile.com", "user_lang", "en", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);


            final HttpMethod method2 = getMethodBuilder().setReferer(fileURL).setBaseURL(SERVER_URL).setActionFromAHrefWhereATagContains("Regular Download").toHttpMethod();
            final String secondUrl = method2.getURI().toString();
            final String id = secondUrl.substring(secondUrl.lastIndexOf("/") + 1);
            if (makeRedirectedRequest(method2)) {
                checkProblems();
                int captchaRetryes = 0;
                while (getContentAsString().contains("Type the characters you see in the picture")) {
                    captchaRetryes++;
                    if (captchaRetryes > 3) {
                        throw new CaptchaEntryInputMismatchException();
                    }
                    CaptchaSupport captchaSupport = getCaptchaSupport();
                    String captchaURL = PlugUtils.getStringBetween(getContentAsString(), "<img alt=\"Captcha\" src=\"", "\"");
                    logger.info("Captcha URL " + captchaURL);
                    String captcha = captchaSupport.getCaptcha(captchaURL);
                    if (captcha == null) {
                        throw new CaptchaEntryInputMismatchException();
                    }
                    final HttpMethod method3 = getMethodBuilder().setReferer(secondUrl).setActionFromFormWhereTagContains("method='post' action='#'", true).setBaseURL(secondUrl).setParameter("captcha_response", captcha).toHttpMethod();
                    if (!makeRedirectedRequest(method3)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                }

                checkProblems();
                final Matcher waitTime = getMatcherAgainstContent("(?:min)?Limit\\s*:\\s*(\\d+)");
                if (!waitTime.find()) {
                    throw new PluginImplementationException("Wait time not found");
                }
                downloadTask.sleep(Integer.parseInt(waitTime.group(1)));

                final HttpMethod method4 = getMethodBuilder().setReferer(secondUrl).setAction("/download/getLinkTimeout/" + id).setBaseURL(SERVER_URL).toGetMethod();
                method4.addRequestHeader("X-Requested-With", "XMLHttpRequest");

                if (makeRedirectedRequest(method4)) {
                    final HttpMethod method5 = getMethodBuilder().setReferer(secondUrl).setActionFromAHrefWhereATagContains("Download").setBaseURL(SERVER_URL).toGetMethod();

                    if (!tryDownloadAndSaveFile(method5)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                } else {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("You have reached the limit of connections")) {
            throw new YouHaveToWaitException("You have reached the limit of connections", PlugUtils.getNumberBetween(contentAsString, "<span id='timeout'>", "</span>"));
        }
        if (contentAsString.contains("From your IP range the limit of connections is reached")) {
            throw new YouHaveToWaitException("From your IP range the limit of connections is reached", PlugUtils.getNumberBetween(contentAsString, "<span id='timeout'>", "</span>"));
        }
        if (contentAsString.contains("File was not found") || contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}