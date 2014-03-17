package cz.vity.freerapid.plugins.services.datacloud;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class DataCloudFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DataCloudFileRunner.class.getName());
    final String BASE_URL = "http://datacloud.to";

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
        PlugUtils.checkName(httpFile, content, "<h1>Download <b>", "<");
        PlugUtils.checkFileSize(httpFile, content, "Size:</span>", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            checkNameAndSize(getContentAsString());
            downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "var waiting_time = ", ";"));

            final Matcher captchaImageMatch = PlugUtils.matcher("<img.+?\"captcha_img\".+?src=\"(.+?)\".+?>", getContentAsString());
            if (!captchaImageMatch.find())
                throw new ErrorDuringDownloadingException("Captcha image not found");
            final Matcher nextPageMatch = PlugUtils.matcher("<a.+?\"verify_captcha\".+?d_data=\"(.+?)\">.+?</a>", getContentAsString());
            if (!nextPageMatch.find())
                throw new ErrorDuringDownloadingException("Next page not found");
            final String nextPage = BASE_URL + nextPageMatch.group(1);
            final Matcher captchaBuilderMatch = PlugUtils.matcher("var Data = \\{\\s+?(action)\\: '(.+?)',\\s+?(.+?)\\: (.+?),\\s+?(link)\\: '(.+?)'\\s+?\\};\\s+?.+?\\s+?url\\: '(.+?)',", getContentAsString());
            if (!captchaBuilderMatch.find())
                throw new ErrorDuringDownloadingException("Captcha data not found");
            do {
                final CaptchaSupport captchaSupport = getCaptchaSupport();
                final String captchaTxt = captchaSupport.getCaptcha(BASE_URL + captchaImageMatch.group(1));
                if (captchaTxt == null)
                    throw new CaptchaEntryInputMismatchException("No Input");
                MethodBuilder captchaBuilder = getMethodBuilder()
                        .setBaseURL(BASE_URL).setReferer(fileURL)
                        .setAction(captchaBuilderMatch.group(7).trim())
                        .setParameter(captchaBuilderMatch.group(1).trim(), captchaBuilderMatch.group(2).trim())
                        .setParameter(captchaBuilderMatch.group(3).trim(), captchaTxt)
                        .setParameter(captchaBuilderMatch.group(5).trim(), captchaBuilderMatch.group(6).trim());
                if (!makeRedirectedRequest(captchaBuilder.toPostMethod())) {
                    throw new ServiceConnectionProblemException();
                }
            } while (!getContentAsString().contains("\"ACTION\":\"OK\""));

            final MethodBuilder nextPageBuilder = getMethodBuilder().setAction(nextPage).setReferer(fileURL);
            if (!makeRedirectedRequest(nextPageBuilder.toGetMethod())) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "var waiting_time = ", ";"));
            final MethodBuilder lastPageBuilder = getMethodBuilder().setActionFromAHrefWhereATagContains("Get Link");
            if (!makeRedirectedRequest(lastPageBuilder.toGetMethod())) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("Download")
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
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
        if (contentAsString.contains("File does not exist") ||
                contentAsString.contains("Page not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}