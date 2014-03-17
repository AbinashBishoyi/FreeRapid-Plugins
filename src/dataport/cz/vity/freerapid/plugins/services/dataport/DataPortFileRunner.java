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
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        Matcher filenameMatcher = PlugUtils.matcher("Název</td>\\s*<td><strong>(.+)</strong></td>", content);
        if (filenameMatcher.find()) {
            httpFile.setFileName(filenameMatcher.group(1));
        } else {
            filenameMatcher = PlugUtils.matcher("Název</td>\\s*<td>(.+)</td>", content);
            if (filenameMatcher.find()) {
                httpFile.setFileName(filenameMatcher.group(1));
            } else {
                throw new PluginImplementationException("File name not found");
            }
        }
        final Matcher filesizeMatcher = PlugUtils.matcher("Velikost</td>\\s*<td>(.+)</td>", content);
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
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            Matcher captchaImageMatcher = PlugUtils.matcher("id=\"captcha_bg\">\\s*<img src=\"(.+)\" height", getContentAsString());
            if (!captchaImageMatcher.find()) {
                throw new PluginImplementationException("Captcha not found");
            }
            final String sCaptchaImage = "http://dataport.cz" + captchaImageMatcher.group(1);
            final String sCaptchaResponse = getCaptchaSupport().getCaptcha(sCaptchaImage);
            if (sCaptchaResponse == null) {
                throw new CaptchaEntryInputMismatchException();
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setActionFromFormByName("free_download_form", true)
                    .setParameter("captchaCode", sCaptchaResponse)
                    .toPostMethod();

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("soubor nebyl nalezen")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Volné sloty pro stažení zdarma jsou v tuhle chvíli vyčerpány.")) {
            throw new YouHaveToWaitException("No Free Slots", 30); //let to know user in FRD
        }


    }

}