package cz.vity.freerapid.plugins.services.filecloudio;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
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

    @Override
    public void run() throws Exception {
        super.run();
        checkFileURL();
        logger.info("Starting download in TASK " + fileURL);
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
                    .setReferer(downloadURL)
                    .setAction(requestUrl)
                    .setParameter("ukey", ukey)
                    .setParameter("__ab1", ab1)
                    .toPostMethod();
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            setFileStreamContentTypes(new String[0], new String[]{"application/json"});
            if (makeRedirectedRequest(httpMethod)) {
                checkDownloadProblems();
                while (getContentAsString().contains("\"captcha\":1")) {
                    stepCaptcha(recaptchaKey, requestUrl, ukey, ab1);
                }
                httpMethod = getMethodBuilder()
                        .setReferer(downloadURL)
                        .setAction(currentURL)
                        .toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkDownloadProblems();
                    throw new ServiceConnectionProblemException();
                }
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
        //
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
            httpFile.setNewURL(new URL(fileURL.replaceFirst("ifile\\.it", "filecloud.io")));
            fileURL = fileURL.replaceFirst("ifile\\.it", "filecloud.io");
        }
    }

}