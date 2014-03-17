package cz.vity.freerapid.plugins.services.rapidgator;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class RapidGatorFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RapidGatorFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        addCookie(new Cookie(".rapidgator.net", "lang", "en", "/", 86400, false));
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
        final String filenameRegexRule = "Downloading:\\s*</strong>\\s*(\\S+)\\s*</p>";
        final String filesizeRegexRule = "File size:\\s*<strong>(.+?)</strong>";

        final Matcher filenameMatcher = PlugUtils.matcher(filenameRegexRule, content);
        if (filenameMatcher.find()) {
            httpFile.setFileName(filenameMatcher.group(1));
        } else {
            throw new PluginImplementationException("File name not found");
        }

        final Matcher filesizeMatcher = PlugUtils.matcher(filesizeRegexRule, content);
        if (filesizeMatcher.find()) {
            PlugUtils.getFileSizeFromString(filesizeMatcher.group(1));
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
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            final int waitTime = PlugUtils.getWaitTimeBetween(contentAsString, "var secs = ", ";", TimeUnit.SECONDS);
            final String fileId = PlugUtils.getStringBetween(contentAsString, "var fid = ", ";");

            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://rapidgator.net/download/AjaxStartTimer")
                    .setParameter("fid", fileId)
                    .toGetMethod();
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new PluginImplementationException();
            }
            checkProblems();
            contentAsString = getContentAsString();
            //logger.info(contentAsString);
            final String sid = PlugUtils.getStringBetween(contentAsString, "\"sid\":\"", "\"}");
            //logger.info(fileId);
            //logger.info(sid);

            downloadTask.sleep(waitTime);

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://rapidgator.net/download/AjaxGetDownloadLink")
                    .setParameter("sid",sid)
                    .toGetMethod();
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new PluginImplementationException();
            }

            //logger.info(getContentAsString());
            
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://rapidgator.net/download/captcha")
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new PluginImplementationException();
            }

            while (getContentAsString().contains("api.recaptcha.net/challenge")) {
                httpMethod = stepCaptcha();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new PluginImplementationException();
                }
            }

            httpMethod = getMethodBuilder()
                    .setAction(PlugUtils.getStringBetween(getContentAsString(), "location.href = '", "';"))
                    .toGetMethod();

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private HttpMethod stepCaptcha() throws Exception {
        //process captcha
        final Matcher reCaptchaKeyMatcher = getMatcherAgainstContent("api\\.recaptcha\\.net/challenge\\?k=(.*?)\">");
        reCaptchaKeyMatcher.find();
        final String reCaptchaKey = reCaptchaKeyMatcher.group(1);
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);

        MethodBuilder methodBuilder = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("http://rapidgator.net/download/captcha")
                .setParameter("DownloadCaptchaForm[captcha]","");

        return r.modifyResponseMethod(methodBuilder).toPostMethod();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Delay between downloads must be not less than")) {
            //final int waitTime = PlugUtils.getWaitTimeBetween(contentAsString,"must be not less than "," min",TimeUnit.MINUTES);
            //throw new YouHaveToWaitException("Delay between downloads must be not less than",waitTime);
            throw new PluginImplementationException("Delay between downloads must be not less than 15 minutes");
        }
        if (contentAsString.contains("You have reached your daily downloads limit")) {
            throw new PluginImplementationException("You have reached your daily downloads limit");
        }
    }

}