package cz.vity.freerapid.plugins.services.fileserve;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author RickCL
 */
class FileserveFilesRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileserveFilesRunner.class.getName());
    private final static String URI_BASE = "http://www.fileserve.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();

        HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());

            Matcher matcher = PlugUtils.matcher("http://(?:www\\.)?fileserve\\.com/[^/]*/(\\w+)", fileURL);
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing download link");
            }
            final String fileKey = matcher.group(1);

            matcher = getMatcherAgainstContent("reCAPTCHA_publickey='([\\w-]*)'");
            if (!matcher.find()) {
                throw new PluginImplementationException("ReCaptcha key not found");
            }
            String recaptcha = "http://www.google.com/recaptcha/api/challenge?k=" + matcher.group(1) + "&ajax=1&cachestop=0." + System.currentTimeMillis();

            do {
                logger.info("Captcha URL: " + recaptcha);
                getMethod = getGetMethod(recaptcha);
                if (!makeRedirectedRequest(getMethod)) {
                    throw new ServiceConnectionProblemException();
                }

                matcher = getMatcherAgainstContent("(?s)challenge\\s*:\\s*'([\\w-]+)'.*?server\\s*:\\s*'([^']+)'");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Error parsing ReCaptcha response");
                }
                String recaptcha_challenge_field = matcher.group(1);
                String captchaImg = matcher.group(2) + "image?c=" + recaptcha_challenge_field;
                logger.info("Captcha URL: " + captchaImg);

                String captcha = getCaptchaSupport().getCaptcha(captchaImg);
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                }

                HttpMethod postMethod = getMethodBuilder().setAction(URI_BASE + "/checkReCaptcha.php").setReferer(fileURL)
                        .setEncodePathAndQuery(true)
                        .setParameter("recaptcha_challenge_field", recaptcha_challenge_field)
                        .setParameter("recaptcha_response_field", captcha)
                        .setParameter("recaptcha_shortencode_field", fileKey)
                        .toPostMethod();
                if (!makeRedirectedRequest(postMethod)) {
                    throw new ServiceConnectionProblemException();
                }
            } while (!getContentAsString().trim().equalsIgnoreCase("success"));

            HttpMethod pMethod = getMethodBuilder().setAction(fileURL).setReferer(fileURL)
                    .setParameter("downloadLink", "wait")
                    .toPostMethod();
            if (!makeRedirectedRequest(pMethod)) {
                throw new ServiceConnectionProblemException();
            }
            try {
                downloadTask.sleep(Integer.parseInt(getContentAsString().trim()) + 1);
            } catch (NumberFormatException e) {
                throw new PluginImplementationException("Waiting time problem", e);
            }
            pMethod = getMethodBuilder().setAction(fileURL).setReferer(fileURL)
                    .setParameter("downloadLink", "show")
                    .toPostMethod();
            if (!makeRedirectedRequest(pMethod)) {
                throw new ServiceConnectionProblemException();
            }

            pMethod = getMethodBuilder().setAction(fileURL).setReferer(fileURL).setParameter("download", "normal").toPostMethod();

            setFileStreamContentTypes("X-LIGHTTPD-send-file");

            if (!tryDownloadAndSaveFile(pMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h1>", "<");
        final Matcher size = getMatcherAgainstContent("<strong>\\s*(\\d.+?)\\s*</strong>");
        if (!size.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size.group(1)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("The file could not be found") || getContentAsString().contains("Page not found") || getContentAsString().contains("File not available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("/error.php")) {
            throw new ServiceConnectionProblemException("Temporary server issue");
        }
        Matcher matcher = getMatcherAgainstContent("You (?:have|need) to wait (\\d+) seconds to start another download");
        if (matcher.find()) {
            throw new YouHaveToWaitException(matcher.group(), Integer.parseInt(matcher.group(1)));
        }
    }

}
