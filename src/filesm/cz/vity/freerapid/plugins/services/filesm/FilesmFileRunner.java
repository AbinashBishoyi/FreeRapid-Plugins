package cz.vity.freerapid.plugins.services.filesm;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FilesmFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilesmFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
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
        final Matcher match = PlugUtils.matcher("<strong>\\s*?(.+?)\\s*?\\((.+?)\\)(<br/>)?\\s*?</strong>", content);
        if (!match.find())
            throw new PluginImplementationException("File name/size not found");
        httpFile.setFileName(match.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);

            downloadTask.sleep(PlugUtils.getNumberBetween(contentAsString, "var seconds = ", ";") + 1);
            method = getGetMethod(PlugUtils.getStringBetween(getContentAsString(), "download-timer').html(\"<a href='", "'>"));
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            boolean captchaLoop = true;
            while (captchaLoop) {
                captchaLoop = false;
                MethodBuilder builder = getMethodBuilder()
                        .setActionFromFormWhereActionContains(fileURL, true)
                        .setAction(fileURL).setReferer(fileURL);
                stepCaptcha(builder);

                if (!tryDownloadAndSaveFile(builder.toPostMethod())) {
                    if (getContentAsString().contains("<li>Captcha confirmation text is invalid.</li>"))
                        captchaLoop = true;
                    else {
                        checkProblems();//if downloading failed
                        throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                    }
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<title>Upload Files - ")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private void stepCaptcha(MethodBuilder mb) throws Exception {
        final Matcher match = getMatcherAgainstContent("recaptcha/api/noscript\\?k=([^\"]+)\"");
        if (!match.find())
            throw new PluginImplementationException("ReCaptcha key not found");
        final String reCaptchaKey = match.group(1);

        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final CaptchaSupport captchaSupport = getCaptchaSupport();

        final String captchaURL = r.getImageURL();
        logger.info("Captcha URL " + captchaURL);
        final String captcha = captchaSupport.getCaptcha(captchaURL);
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        r.setRecognized(captcha);
        r.modifyResponseMethod(mb);
    }
}