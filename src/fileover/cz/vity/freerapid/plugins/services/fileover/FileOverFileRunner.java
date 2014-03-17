package cz.vity.freerapid.plugins.services.fileover;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FileOverFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileOverFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
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
        PlugUtils.checkName(httpFile, content, "break-word;\">", "</h2>");
        if (!content.contains("<h3>You have to wait"))
            PlugUtils.checkFileSize(httpFile, content, "File Size: ", "</h3>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            String contentAsString = getContentAsString();
            checkFileProblems();
            checkNameAndSize(contentAsString);
            checkDownloadProblems();

            final Matcher matcher = PlugUtils.matcher("fileover\\.net/([^/]+)", fileURL);
            if (!matcher.find())
                throw new PluginImplementationException("Bad file URL");
            final String fid = matcher.group(1);
            if (contentAsString.contains("class=\"waitline\"")) {
                downloadTask.sleep(PlugUtils.getNumberBetween(contentAsString, "<span class=\"wseconds\">", "</span>"));
            }
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(getBaseURL() + "/ax/timereq.flo?" + fid)
                    .toGetMethod();
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod)) {
                checkDownloadProblems();
                throw new ServiceConnectionProblemException();
            }

            checkDownloadProblems();
            contentAsString = getContentAsString();
            final String hash = PlugUtils.getStringBetween(contentAsString, "\"hash\":\"", "\"");
            do {
                httpMethod = stepCaptcha(fid, hash);
                if (!makeRedirectedRequest(httpMethod)) {
                    checkDownloadProblems();
                    throw new ServiceConnectionProblemException();
                }
                logger.info(getContentAsString());
            }
            while (getContentAsString().contains("recaptcha/api/challenge") || getContentAsString().contains("Not Ready")); //"477 Not Ready" : reask new link if server generates the message, rarely happens.

            checkDownloadProblems();
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(PlugUtils.getStringBetween(getContentAsString(), "<a href=\"", "\">"))
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkDownloadProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkFileProblems();
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private HttpMethod stepCaptcha(String fid, String hash) throws Exception {
        MethodBuilder methodBuilder = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(getBaseURL() + "/ax/timepoll.flo?file=" + fid + "&hash=" + hash);
        final HttpMethod httpMethod = methodBuilder.toGetMethod();
        httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
        if (!makeRedirectedRequest(httpMethod)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }

        methodBuilder.setParameter("submit", "Submit");
        final Matcher reCaptchaKeyMatcher = getMatcherAgainstContent("recaptcha/api/challenge\\?k=(.*?)\">");
        if (!reCaptchaKeyMatcher.find())
            throw new PluginImplementationException("Recaptcha not found");
        final String reCaptchaKey = reCaptchaKeyMatcher.group(1);
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);

        return r.modifyResponseMethod(methodBuilder).toPostMethod();
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file was completely removed")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<h3>You have to wait")) {
            String regexRule = "(?:([0-9]+) hours? )?(?:([0-9]+) minutes? )?(?:([0-9]+) seconds?)";
            Matcher matcher = PlugUtils.matcher(regexRule, contentAsString);
            int waitHours = 0, waitMinutes = 0, waitSeconds = 0, waitTime;
            if (matcher.find()) {
                if (matcher.group(1) != null)
                    waitHours = Integer.parseInt(matcher.group(1));
                if (matcher.group(2) != null)
                    waitMinutes = Integer.parseInt(matcher.group(2));
                waitSeconds = Integer.parseInt(matcher.group(3));
            }
            waitTime = (waitHours * 60 * 60) + (waitMinutes * 60) + waitSeconds;
            throw new YouHaveToWaitException("You have to wait " + waitTime + " seconds", waitTime);
        }
    }

}