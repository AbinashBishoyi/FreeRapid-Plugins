package cz.vity.freerapid.plugins.services.uploking;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
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
class UploKingFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploKingFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        addCookie(new Cookie(".uploking.com", "uploking_lang", "2", "/", 86400, false));
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
        final Matcher match = PlugUtils.matcher("<div id=\"DownloadFileName\">\\s+?(.+?)<.+?>\\|(.+?)<", content);
        if (!match.find())
            throw new PluginImplementationException("File name/size not found");
        httpFile.setFileName(match.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(2).trim()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(".uploking.com", "uploking_lang", "2", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            checkNameAndSize(getContentAsString());//extract file name and size from the page

            final String actionBox = "ajax.php?mode=box&function=";
            final String actionCheck = "ajax.php?mode=check&function=";

            final Matcher matchFullUrl = PlugUtils.matcher("window.location.href='.+?=(.+?)'\"><div class=\"link_slow\">", getContentAsString());
            if (!matchFullUrl.find())
                throw new PluginImplementationException("err 1");
            final String fullUrl = matchFullUrl.group(1);

            final Matcher matchKey = PlugUtils.matcher("<input.+?id=\"fk\".+?value=\"(.+?)\"", getContentAsString());
            if (!matchKey.find())
                throw new PluginImplementationException("err 2");
            final String keyValue = matchKey.group(1);

            final HttpMethod httpM1 = getMethodBuilder()
                    .setReferer(fullUrl)
                    .setAction(actionBox + "predownload&fk=" + keyValue)
                    .setAjax()
                    .toGetMethod();
            if (!makeRedirectedRequest(httpM1)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Problem loading wait page");
            }
            final Matcher matchWait = PlugUtils.matcher("var wait = parseInt\\(([^\\.\\)]+?)[\\.\\)]", getContentAsString());
            if (!matchWait.find())
                throw new PluginImplementationException("Wait time not found");
            final int waitTime = Integer.parseInt(matchWait.group(1));
            if (waitTime > 600)
                throw new YouHaveToWaitException("You Have To Wait", waitTime);
            downloadTask.sleep(waitTime + 1);

            final HttpMethod httpM2 = getMethodBuilder()
                    .setReferer(fullUrl)
                    .setAction(actionBox + "captcha&fk=" + keyValue)
                    .setAjax()
                    .toGetMethod();
            if (!makeRedirectedRequest(httpM2)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Problem loading captcha page");
            }
            final MethodBuilder builder = getMethodBuilder()
                    .setReferer(fullUrl)
                    .setAction(actionCheck + "freedownload&fk=" + keyValue);
            do {
                if (!makeRedirectedRequest(stepCaptcha(builder))) {
                    throw new ServiceConnectionProblemException();
                }
            } while (!getContentAsString().contains("\"status\":\"ok\""));

            final String downloadUrl = PlugUtils.getStringBetween(getContentAsString(), "\"url\":\"", "\"}");
            //here is the download link extraction
            if (!tryDownloadAndSaveFile(getGetMethod(downloadUrl.replaceAll("\\\\/", "/")))) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File was not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private HttpMethod stepCaptcha(MethodBuilder builder) throws Exception {
        final Matcher matcher = PlugUtils.matcher("Recaptcha\\.create\\((.+?)\",", getContentAsString());
        if (!matcher.find()) {
            throw new PluginImplementationException("ReCaptcha key not found");
        }
        String reCaptchaKey = matcher.group(1);
        reCaptchaKey = reCaptchaKey.substring(2, reCaptchaKey.length() - 1);

        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final CaptchaSupport captchaSupport = getCaptchaSupport();

        final String captchaURL = r.getImageURL();
        logger.info("Captcha URL " + captchaURL);

        final String captcha = captchaSupport.getCaptcha(captchaURL);
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        r.setRecognized(captcha);

        final HttpMethod httpMethod = r.modifyResponseMethod(builder).toPostMethod();
        httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");//use AJAX
        return httpMethod;
    }
}