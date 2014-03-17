package cz.vity.freerapid.plugins.services.dataport;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Vity , birchie
 */
class DataPortFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DataPortFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        checkUrl();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkUrl() {
        if (fileURL.contains("://dataport.cz"))
            fileURL = fileURL.replaceFirst("://dataport.cz", "://www.dataport.cz");
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<span itemprop=\"name\">", "</span></td>");
        final Matcher fileSizeMatcher = PlugUtils.matcher("Velikost</td>\\s*<td>(.+)</td>", content);
        if (!fileSizeMatcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSizeMatcher.group(1)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkUrl();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            int captchaLoopCount = 0;
            boolean captchaLoop = true;
            while (captchaLoop) {
                captchaLoop = false;
                if (captchaLoopCount++ > 10)
                    throw new PluginImplementationException("Captcha attempt limit exceeded");
                final Matcher captchaImageMatcher = PlugUtils.matcher("id=\"captcha_bg\">\\s*<img src=\"(.+)\" height", getContentAsString());
                if (!captchaImageMatcher.find()) {
                    throw new PluginImplementationException("Captcha not found");
                }
                final String sCaptchaImage = "http://www.dataport.cz" + captchaImageMatcher.group(1);
                final String sCaptchaResponse = getCaptchaSupport().getCaptcha(sCaptchaImage);
                if (sCaptchaResponse == null) {
                    throw new CaptchaEntryInputMismatchException();
                }
                final HttpMethod httpMethod = getMethodBuilder()
                        .setBaseURL("http://www.dataport.cz")
                        .setActionFromFormByName("free_download_form", true)
                        .setParameter("captchaCode", sCaptchaResponse)
                        .setReferer(fileURL)
                        .toPostMethod();
                //here is the download link extraction
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    if (getContentAsString().contains("<section id=\"captcha_bg\">"))
                        captchaLoop = true;
                    else
                        throw new PluginImplementationException("error saving file");//some unknown problem
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("soubor nebyl nalezen") || contentAsString.contains("Tento soubor neexistuje")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Volné sloty pro stažení zdarma jsou v tuhle chvíli vyčerpány.") ||
                contentAsString.contains("momentálně nejsou k dispozici žádné free download sloty")) {
            throw new YouHaveToWaitException("No Free Slots", 30); //let to know user in FRD
        }
    }

}