package cz.vity.freerapid.plugins.services.mixturecloud;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
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
 * @author tong2shot
 */
class MixtureCloudFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MixtureCloudFileRunner.class.getName());
    private final static String SERVICE_COOKIE_DOMAIN = ".mixturecloud.com";

    private String checkURL(String fileURL) {
        return fileURL.replaceAll("/video=", "/download=");
    }

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "en", "/", null, false));
        fileURL = checkURL(fileURL);
        logger.info(fileURL);
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
        PlugUtils.checkName(httpFile, content, "<h2>Download : ", "</h2>");
        PlugUtils.checkFileSize(httpFile, content, "Original size : <span style=\"font-weight:bold\">", "</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "lang", "en", "/", null, false));
        fileURL = checkURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            checkNameAndSize(getContentAsString());//extract file name and size from the page

            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormByName("slowDownload", true)
                    .setAction(fileURL)
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new PluginImplementationException();
            }

            checkProblems();

            while (getContentAsString().contains("recaptcha/api/challenge")) {
                MethodBuilder methodBuilder = getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromFormByName("reCaptcha", true)
                        .setAction(fileURL);
                httpMethod = stepCaptcha(methodBuilder);

                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new PluginImplementationException();
                }
            }

            checkProblems();
            final int waitTime = PlugUtils.getNumberBetween(getContentAsString(), "for (i=", ";i");
            final String downloadURL = "http://www.mixturecloud.com/" + PlugUtils.getStringBetween(getContentAsString(), "href=\\\"", "\\\">Download</a>\";");
            downloadTask.sleep(waitTime);

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadURL)
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

    private HttpMethod stepCaptcha(MethodBuilder methodBuilder) throws Exception {
        final Matcher reCaptchaKeyMatcher = getMatcherAgainstContent("recaptcha/api/challenge\\?k=(.*?)\">");
        if (!reCaptchaKeyMatcher.find()) {
            throw new PluginImplementationException("ReCaptcha key not found");
        }
        final String reCaptchaKey = reCaptchaKeyMatcher.group(1);

        logger.info("recaptcha public key : " + reCaptchaKey);

        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);
        return r.modifyResponseMethod(methodBuilder)
                .toPostMethod();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("we removed the file")) {
            throw new URLNotAvailableAnymoreException("File was removed"); //let to know user in FRD
        }
        if (contentAsString.contains("No videos at this address")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("<h3>You have to wait")) {
            throw new PluginImplementationException("You have to wait 1200 seconds to start a new download");
        }
    }

}