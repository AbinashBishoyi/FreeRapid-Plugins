package cz.vity.freerapid.plugins.services.filelaser;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FileLaserFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileLaserFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
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
        PlugUtils.checkName(httpFile, content, "<h3>", "<strong>[");
        PlugUtils.checkFileSize(httpFile, content, "[</strong>", "<strong>]");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        final String freeURL = fileURL + "/free/";
        client.setReferer(fileURL);
        logger.info("Starting download in TASK " + freeURL);
        final GetMethod method = getGetMethod(freeURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);

            HttpMethod httpMethod;
            final int waitTime = PlugUtils.getNumberBetween(getContentAsString(), "var time =", ";");
            downloadTask.sleep(waitTime + 1);
            while (true) {
                final MethodBuilder methodBuilder = getMethodBuilder()
                        .setReferer(freeURL)
                        .setActionFromFormWhereTagContains("captchaForm", true)
                        .setAction(freeURL);
                httpMethod = stepReCaptcha(methodBuilder);
                final int httpStatus = client.makeRequest(httpMethod, false);
                if (httpStatus / 100 == 3) { //redirect to download file
                    final Header locationHeader = httpMethod.getResponseHeader("Location");
                    if (locationHeader == null)
                        throw new PluginImplementationException("Could not find download file location");
                    httpMethod = getMethodBuilder()
                            .setReferer(fileURL)
                            .setAction(locationHeader.getValue())
                            .toGetMethod();
                    break;
                } else {
                    if (!getContentAsString().contains("recaptcha/api/challenge")) {
                        checkProblems();
                        throw new PluginImplementationException("Download link not found");
                    }
                }
            }
            checkProblems();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file was not found or has been removed")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("requested file is currently unavailable")) {
            throw new PluginImplementationException("The requested file is currently unavailable. Please try again later.");
        }
        if (contentAsString.contains("may download another file in")) {
            String regexRule = "(?:([0-9]+) hours?, )?(?:([0-9]+) minutes?, )?(?:([0-9]+) seconds?)";
            Matcher matcher = PlugUtils.matcher(regexRule, contentAsString);
            int waitHours = 0, waitMinutes = 0, waitSeconds = 0, waitTime;
            if (matcher.find()) {
                if (matcher.group(1) != null)
                    waitHours = Integer.parseInt(matcher.group(1));
                if (matcher.group(2) != null)
                    waitMinutes = Integer.parseInt(matcher.group(2));
                waitSeconds = Integer.parseInt(matcher.group(3));
            }
            waitTime = (waitHours * 60 * 60) + (waitMinutes * 60) + waitSeconds + 1;
            throw new YouHaveToWaitException("You may download another file in " + waitTime + " seconds", waitTime);
        }
    }

    private HttpMethod stepReCaptcha(MethodBuilder methodBuilder) throws Exception {
        final Matcher reCaptchaKeyMatcher = getMatcherAgainstContent("recaptcha/api/challenge\\?k=(.*?)\"");
        if (!reCaptchaKeyMatcher.find())
            throw new PluginImplementationException("Recaptcha key not found");
        final String reCaptchaKey = reCaptchaKeyMatcher.group(1);
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);

        return r.modifyResponseMethod(methodBuilder).toPostMethod();
    }


}