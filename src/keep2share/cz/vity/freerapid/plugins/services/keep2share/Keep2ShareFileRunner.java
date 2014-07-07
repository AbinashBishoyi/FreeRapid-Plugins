package cz.vity.freerapid.plugins.services.keep2share;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
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
class Keep2ShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Keep2ShareFileRunner.class.getName());
    private String baseUrl = "http://keep2share.cc";

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        addCookie(new Cookie(".keep2share.cc", "lang", "en", "/", 86400, false));
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
        final Matcher match = PlugUtils.matcher("<img width.+?>\\s*?(.+?)\\s*?</span>", content);
        if (match.find()) {
            httpFile.setFileName(match.group(1).trim());
            PlugUtils.checkFileSize(httpFile, content, "File size", "</div>");
        } else {
            PlugUtils.checkName(httpFile, content, "File: <span>", "<");
            PlugUtils.checkFileSize(httpFile, content, "Size:", "<");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(".keep2share.cc", "lang", "en", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            if (!contentAsString.contains("This link will be available for")) {
                fileURL = method.getURI().getURI();
                baseUrl = method.getURI().getURI().split("/file/")[0];
                final MethodBuilder aMethod = getMethodBuilder()
                        .setBaseURL(baseUrl).setReferer(fileURL)
                        .setActionFromFormWhereTagContains("slow_id", true);
                if (!makeRedirectedRequest(aMethod.toPostMethod())) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                if (getContentAsString().contains("window.location.href")) {
                    HttpMethod hMethod = getMethodBuilder()
                            .setAction(PlugUtils.getStringBetween(getContentAsString(), "window.location.href = '", "';"))
                            .toGetMethod();
                    if (!tryDownloadAndSaveFile(hMethod)) {
                        checkProblems();//if downloading failed
                        throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                    }
                    return;
                }
                do {
                    final MethodBuilder captchaMethod = getMethodBuilder()
                            .setBaseURL(baseUrl)
                            .setActionFromFormWhereTagContains("Slow download", true);
                    if (!makeRedirectedRequest(doCaptcha(captchaMethod).toPostMethod())) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();
                } while (getContentAsString().contains("The verification code is incorrect"));

                final Matcher match = PlugUtils.matcher("<div id=\"download-wait-timer\">\\s*?(.+?)\\s*?</div>", getContentAsString());
                if (!match.find())
                    throw new PluginImplementationException("Wait time not found");
                downloadTask.sleep(1 + Integer.parseInt(match.group(1).trim()));
                final MethodBuilder dlBuilder = getMethodBuilder()
                        .setBaseURL(baseUrl)
                        .setAjax()
                        .setAction(PlugUtils.getStringBetween(getContentAsString(), "url: '", "',"));
                final String[] params = PlugUtils.getStringBetween(getContentAsString(), "data: {", "},").split(",");
                for (String p : params) {
                    final String[] param = p.split(":");
                    dlBuilder.setParameter(param[0].replaceAll("'", "").trim(), param[1].replaceAll("'", "").trim());
                }
                if (!makeRedirectedRequest(dlBuilder.toPostMethod())) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            }
            final HttpMethod httpMethod = getGetMethod(baseUrl + PlugUtils.getStringBetween(getContentAsString(), "<a href=\"", "\">this link"));
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
        final String content = getContentAsString();
        if (content.contains("File not found") || content.contains("<title>Error 404</title>")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (content.contains("File size to large") ||
                content.contains("Free user can't download large files")) {
            throw new NotRecoverableDownloadException("This file size is only for Premium members");
        }
        if (content.contains("only for premium members")) {
            throw new NotRecoverableDownloadException("This file is only for Premium members");
        }
        final Matcher waitMatch = PlugUtils.matcher("Please wait (\\d+?):(\\d+?):(\\d+?) to download this file", content);
        if (waitMatch.find()) {
            final int waitTime = Integer.parseInt(waitMatch.group(3)) + 60 * (Integer.parseInt(waitMatch.group(2)) + 60 * Integer.parseInt(waitMatch.group(1)));
            throw new YouHaveToWaitException("Please wait for download", waitTime);
        }
    }

    private MethodBuilder doCaptcha(final MethodBuilder builder) throws Exception {
        final HttpMethod newCaptcha = getMethodBuilder().setBaseURL(baseUrl).setAction("/file/captcha.html?refresh=1").setAjax().toGetMethod();
        if (!makeRedirectedRequest(newCaptcha)) {
            throw new ServiceConnectionProblemException();
        }
        final String captchaSrc = getMethodBuilder().setBaseURL(baseUrl).setAction(PlugUtils.getStringBetween(getContentAsString(), "url\":\"", "\"").replace("\\", "")).getEscapedURI();
        final String captchaTxt = getCaptchaSupport().getCaptcha(captchaSrc);
        if (captchaTxt == null)
            throw new CaptchaEntryInputMismatchException();
        return builder.setParameter("CaptchaForm[code]", captchaTxt);
    }

}