package cz.vity.freerapid.plugins.services.rapidgator;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.solvemediacaptcha.SolveMediaCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot, birchie
 */
class RapidGatorFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RapidGatorFileRunner.class.getName());
    private final static long CAPTCHA_TIMEOUT = 25; // tried 30, but still caught captcha expired

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".rapidgator.net", "lang", "en", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            if (fileURL.contains("/folder/")) {
                httpFile.setFileName("Folder : " + PlugUtils.getStringBetween(getContentAsString(), "<title>Download file", "</title>"));
                httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
            } else
                checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final String filenameRegexRule = "Downloading:\\s*</strong>\\s*<a.+?>\\s*(\\S+)\\s*</a>";
        final String filesizeRegexRule = "File size:\\s*<strong>(.+?)</strong>";
        final Matcher filenameMatcher = PlugUtils.matcher(filenameRegexRule, content);
        if (filenameMatcher.find()) {
            httpFile.setFileName(filenameMatcher.group(1));
        } else {
            throw new PluginImplementationException("File name not found");
        }
        final Matcher filesizeMatcher = PlugUtils.matcher(filesizeRegexRule, content);
        if (filesizeMatcher.find()) {
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(filesizeMatcher.group(1)));
        } else {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(".rapidgator.net", "lang", "en", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            String contentAsString = getContentAsString();
            checkProblems();
            if (fileURL.contains("/folder/")) {
                List<URI> list = new LinkedList<URI>();
                final Matcher m = PlugUtils.matcher("class=\"(?:odd|even)\"><td><a href=\"(.+?)\"", getContentAsString());
                while (m.find()) {
                    list.add(new URI("http://rapidgator.net" + m.group(1).trim()));
                }
                if (list.isEmpty()) throw new PluginImplementationException("No links found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                httpFile.setFileName("Link(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);
                return;
            }
            checkNameAndSize(contentAsString);
            final int waitTime = PlugUtils.getWaitTimeBetween(contentAsString, "var secs =", ";", TimeUnit.SECONDS);
            final String fileId = PlugUtils.getStringBetween(contentAsString, "var fid =", ";");
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://rapidgator.net/download/AjaxStartTimer")
                    .setParameter("fid", fileId)
                    .toGetMethod();
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            contentAsString = getContentAsString();
            final String sid = PlugUtils.getStringBetween(contentAsString, "\"sid\":\"", "\"}");
            downloadTask.sleep(waitTime + 1);
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://rapidgator.net/download/AjaxGetDownloadLink")
                    .setParameter("sid", sid)
                    .toGetMethod();
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://rapidgator.net/download/captcha")
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            int captchaCounter = 0;
            while (getContentAsString().contains("/download/captcha")) {
                if (captchaCounter++ > 8) {
                    throw new PluginImplementationException("Unable to solve captcha");
                }
                httpMethod = stepCaptcha();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            }
            checkProblems();
            Matcher match = PlugUtils.matcher("return '(.+?)';", getContentAsString());  //skip download manager
            do {
                if (!match.find())
                    throw new PluginImplementationException("Download link not found");
            } while (match.group(1).contains("rapidgatordownloader"));

            httpMethod = getMethodBuilder()
                    .setAction(match.group(1))
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private HttpMethod stepCaptcha() throws Exception {
        if (getContentAsString().contains("NoScript.aspx")) {
            logger.info("Captcha Type 1");
            HttpMethod method = getMethodBuilder()
                    .setReferer("http://rapidgator.net/download/captcha")
                    .setActionFromIFrameSrcWhereTagContains("NoScript.aspx")
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final Matcher codeMatcher = PlugUtils.matcher("<td\\s*class=\"code\">(.+)</td>", getContentAsString());
            if (!codeMatcher.find()) {
                throw new PluginImplementationException("Captcha code not found");
            }
            final String codeTxt = codeMatcher.group(1);
            final Matcher challengeMatcher = PlugUtils.matcher("img\\s*src=\"(.+)\"\\s*width", getContentAsString());
            if (!challengeMatcher.find()) {
                throw new PluginImplementationException("Captcha challenge not found");
            }
            final String challengeImg = challengeMatcher.group(1);
            final CaptchaSupport captchaSupport = getCaptchaSupport();
            final String captchaTxt = captchaSupport.getCaptcha(challengeImg);
            if (captchaTxt == null) throw new CaptchaEntryInputMismatchException("No Input");
            MethodBuilder methodBuilder = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://rapidgator.net/download/captcha")
                    .setParameter("adscaptcha_challenge_field", codeTxt)
                    .setParameter("adscaptcha_response_field", captchaTxt)
                    .setParameter("DownloadCaptchaForm[captcha]", "");
            return methodBuilder.toPostMethod();
        } else if (getContentAsString().contains("papi/challenge.noscript")) {
            logger.info("Captcha Type 2");
            final Matcher captchaKeyMatcher = getMatcherAgainstContent("papi/challenge\\.noscript\\?k=(.*?)\"");
            if (!captchaKeyMatcher.find()) throw new PluginImplementationException("Captcha not found");
            final String captchaKey = captchaKeyMatcher.group(1);
            final long captchaStartTime = System.currentTimeMillis();
            SolveMediaCaptcha solveMediaCaptcha = new SolveMediaCaptcha(captchaKey, client, getCaptchaSupport());
            solveMediaCaptcha.askForCaptcha();
            if (((System.currentTimeMillis() - captchaStartTime) / 1000) > CAPTCHA_TIMEOUT)
                throw new YouHaveToWaitException("Retry request to avoid captcha expired", 5);
            final MethodBuilder methodBuilder = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://rapidgator.net/download/captcha")
                    .setParameter("DownloadCaptchaForm[captcha]", "");
            return solveMediaCaptcha.modifyResponseMethod(methodBuilder).toPostMethod();
        } else {
            logger.info("Captcha Error");
            checkProblems();
            throw new PluginImplementationException("Captcha not found");
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found") ||
                contentAsString.contains("<title>Rapidgator.net: Fast, safe and secure file hosting</title>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Delay between downloads must be not less than")) {
            final String waitTime = PlugUtils.getStringBetween(contentAsString, "must be not less than", "min");
            throw new YouHaveToWaitException("Delay between downloads must be not less than " + waitTime + " minutes", 900);
        }
        if (contentAsString.contains("You have reached your daily downloads limit")) {
            throw new NotRecoverableDownloadException("You have reached your daily downloads limit");
        }
        if (contentAsString.contains("You have reached your hourly downloads limit")) {
            throw new YouHaveToWaitException("You have reached your hourly downloads limit", 15 * 60);
        }
        final Matcher uptoMatcher = getMatcherAgainstContent("You can download files up to (.+?) in free mode");
        if (uptoMatcher.find()) {
            throw new NotRecoverableDownloadException("You can download files up to " + uptoMatcher.group(1) + " in free mode");
        }
        if (contentAsString.contains("You can`t download not more than")) {
            throw new NotRecoverableDownloadException("You can`t download not more than 1 file at a time in free mode");
        }
        if (contentAsString.contains("Captcha expired")) {
            throw new YouHaveToWaitException("Captcha expired. Try again in 15 minutes", 300);
        }
        if (contentAsString.contains("file can be downloaded by premium")) {
            throw new NotRecoverableDownloadException("This file can be downloaded by premium only");
        }
    }


}