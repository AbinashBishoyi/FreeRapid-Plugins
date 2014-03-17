package cz.vity.freerapid.plugins.services.nitrobits;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.solvemediacaptcha.SolveMediaCaptcha;
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
 * @since 0.9u3
 */
class NitroBitsFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NitroBitsFileRunner.class.getName());

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
        Matcher matcher = PlugUtils.matcher("<h1>(.+?) - ([0-9,\\.]+ (?:KB|MB|GB)).*?</h1>", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("File name/size not found");
        }
        String filename = matcher.group(1);
        long filesize = PlugUtils.getFileSizeFromString(matcher.group(2));
        logger.info("File name : " + filename);
        logger.info("File size : " + filesize);
        httpFile.setFileName(filename);
        httpFile.setFileSize(filesize);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());

            Matcher matcher = PlugUtils.matcher("/file/([^/]+)", fileURL);
            if (!matcher.find()) {
                throw new PluginImplementationException("File id not found");
            }
            String fileId = matcher.group(1);
            String downloadSession;
            int waitTime;

            try {
                downloadSession = PlugUtils.getStringBetween(getContentAsString(), "var downsess = \"", "\"");
            } catch (PluginImplementationException e) {
                throw new PluginImplementationException("Download session not found");
            }

            try {
                waitTime = PlugUtils.getNumberBetween(getContentAsString(), "var wait = ", ";");
            } catch (PluginImplementationException e) {
                throw new PluginImplementationException("Wait time not found");
            }

            matcher = getMatcherAgainstContent("challenge\\.noscript\\?k=(.+?)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Captcha key not found");
            }
            String captchaKey = matcher.group(1);

            downloadTask.sleep(waitTime + 1);
            do {
                MethodBuilder mb = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAjax()
                        .setAction("http://nitrobits.com/ajax.solvcaptcha.php")
                        .setParameter("downsess", downloadSession);
                if (!makeRedirectedRequest(stepCaptcha(mb, captchaKey).toPostMethod())) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            } while (getContentAsString().contains("{\"status\":\"ERROR\"}"));

            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAjax()
                    .setAction("http://nitrobits.com/getdownlink.php")
                    .setParameter("down_sess", downloadSession)
                    .setParameter("file", fileId)
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            String downloadUrl;
            try {
                downloadUrl = PlugUtils.getStringBetween(getContentAsString(), "\"link\":\"", "\"").replace("\\/", "/");
            } catch (PluginImplementationException e) {
                throw new PluginImplementationException("Download link not found");
            }
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadUrl)
                    .toGetMethod();
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
        if (contentAsString.contains("File is not available")
                || contentAsString.contains("The requested file was not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("Your download will start in")) {
            String strWaitTime[] = PlugUtils.getStringBetween(contentAsString, "id=\"timer\">", "</span>").split(":");
            int waitTime = (Integer.parseInt(strWaitTime[0]) * 60) + (Integer.parseInt(strWaitTime[1])) + 1;
            throw new YouHaveToWaitException("You have to wait " + waitTime, waitTime);

        }
    }

    private MethodBuilder stepCaptcha(MethodBuilder mb, String captchaKey) throws Exception {
        SolveMediaCaptcha solveMediaCaptcha = new SolveMediaCaptcha(captchaKey, client, getCaptchaSupport());
        solveMediaCaptcha.askForCaptcha();
        solveMediaCaptcha.modifyResponseMethod(mb);
        return mb;
    }

}
