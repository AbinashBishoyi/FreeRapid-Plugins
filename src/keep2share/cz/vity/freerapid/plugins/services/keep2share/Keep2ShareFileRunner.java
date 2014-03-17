package cz.vity.freerapid.plugins.services.keep2share;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
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
 * @author birchie
 */
class Keep2ShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Keep2ShareFileRunner.class.getName());
    final String baseUrl = "http://keep2share.cc";

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
            PlugUtils.checkName(httpFile, content, "<div class=\"name\">File: <span>", "</span></div>");
            PlugUtils.checkFileSize(httpFile, content, "<div class=\"size\">Size:", "</div>");
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
            if (!contentAsString.contains(" window.location.href = '")) {
                boolean loopCaptcha = true;
                while (loopCaptcha) {
                    loopCaptcha = false;
                    final MethodBuilder captchaMethod = getMethodBuilder().setReferer(fileURL)
                            .setActionFromFormWhereTagContains("Slow download", true);
                    if (!makeRedirectedRequest(doCaptcha(captchaMethod).toPostMethod())) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();
                    if (getContentAsString().contains("The verification code is incorrect"))
                        loopCaptcha = true;
                }
                downloadTask.sleep(1 + PlugUtils.getNumberBetween(getContentAsString(), "you have to wait:", "seconds"));
                final MethodBuilder dlBuilder = getMethodBuilder()
                        .setReferer(fileURL)
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
            final HttpMethod httpMethod = getGetMethod(baseUrl + PlugUtils.getStringBetween(getContentAsString(), "window.location.href = '", "';"));
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
        if (content.contains("File size to large")) {
            throw new NotRecoverableDownloadException("This file is only for Premium members");
        }
    }

    private MethodBuilder doCaptcha(final MethodBuilder builder) throws Exception {
        final String imgUrl = baseUrl + "/file/captcha" + PlugUtils.getStringBetween(getContentAsString(), "src=\"/file/captcha", "\"");
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaTxt = captchaSupport.getCaptcha(imgUrl);
        if (captchaTxt == null)
            throw new CaptchaEntryInputMismatchException();
        builder.setParameter("CaptchaForm[code]", captchaTxt);
        return builder;
    }

}