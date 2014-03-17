package cz.vity.freerapid.plugins.services.turbobit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Thumb
 */
class TurboBitFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TurboBitFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize();//ok let's extract file name and size from the page
        } else
            throw new ServiceConnectionProblemException();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher nameMatcher = PlugUtils.matcher("??????? ????(?:.*?)<b>(.*?)</b>", getContentAsString());
        if (!nameMatcher.find())
            unimplemented();
        httpFile.setFileName(nameMatcher.group(1));

        Matcher sizeMatcher = PlugUtils.matcher("?????? ?????:(?:\\s|<[^>]*>)*([^>]*)<", getContentAsString());
        if (!sizeMatcher.find())
            unimplemented();
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(sizeMatcher.group(1)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        runCheck();
        checkDownloadProblems();

        final HttpMethod method1 = getMethodBuilder()
                .setActionFromAHrefWhereATagContains("??????? ?????????")
                .setBaseURL("http://www.turbobit.net/")
                .toHttpMethod();

        if (!makeRequest(method1))
            throw new ServiceConnectionProblemException();
        checkProblems();

        HttpMethod method2 = getMethodBuilder()
                .setActionFromFormWhereTagContains("???????", true)
                .setParameter("captcha_response", getCaptcha())
                .setBaseURL(method1.getURI().toString())
                .toHttpMethod();

        if (!makeRequest(method2))
            throw new ServiceConnectionProblemException();
        checkProblems();

        Matcher actionMatcher = PlugUtils.matcher("\"(/download/timeout[^\"]*)\"", getContentAsString());
        if (!actionMatcher.find())
            unimplemented();

        HttpMethod method3 = getMethodBuilder()
                .setAction(actionMatcher.group(1))
                .setBaseURL("http://www.turbobit.net")
                .toHttpMethod();

        downloadTask.sleep(62);

        if (!makeRequest(method3))
            throw new ServiceConnectionProblemException();
        checkProblems();

        HttpMethod method4 = getMethodBuilder()
                .setActionFromAHrefWhereATagContains("??????? ????")
                .setBaseURL("http://www.turbobit.net")
                .toHttpMethod();

        //here is the download link extraction
        if (!tryDownloadAndSaveFile(method4)) {
            checkProblems();//if downloading failed
            unimplemented();
        }
    }

    /**
     * @return
     * @throws ErrorDuringDownloadingException
     *
     */
    private String getCaptcha() throws ErrorDuringDownloadingException {
        CaptchaSupport cs = getCaptchaSupport();
        Matcher captchaURLMatcher = PlugUtils.matcher("<img alt=\"Captcha\" src=\"([^\"]*)\"", getContentAsString());
        if (!captchaURLMatcher.find())
            unimplemented();
        return cs.getCaptcha(captchaURLMatcher.group(1));
    }

    /**
     * @throws ErrorDuringDownloadingException
     *
     */
    private void unimplemented() throws ErrorDuringDownloadingException {
        logger.warning(getContentAsString());//log the info
        throw new PluginImplementationException();//some unknown problem
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        Matcher err404Matcher = PlugUtils.matcher("<div class=\"text-404\">(.*?)</div", getContentAsString());
        if (err404Matcher.find()) {
            if (err404Matcher.group(1).contains("??????????? ???????? ?? ??????"))
                throw new URLNotAvailableAnymoreException(err404Matcher.group(1));
        }
        if (getContentAsString().contains("???? ?? ??????"))
            throw new URLNotAvailableAnymoreException();
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        try {
            Matcher waitMatcher = PlugUtils.matcher("??????????\\s+?????????.*<span id='timeout'>([^>]*)<", getContentAsString());
            if (waitMatcher.find()) {
                throw new YouHaveToWaitException("You have to wait", Integer.valueOf(waitMatcher.group(1)));
            }
            Matcher errMatcher = PlugUtils.matcher("<div[^>]*class='error'[^>]*>([^<]*)<", getContentAsString());
            if (errMatcher.find() && !errMatcher.group(1).isEmpty()) {
                if (errMatcher.group(1).contains("???????? ?????"))
                    throw new CaptchaEntryInputMismatchException();
                unimplemented();
            }
            if (getContentAsString().contains("?????? ??????????")) // it's unlikely we get this...
                throw new YouHaveToWaitException("Trying again...", 10);
        } catch (NumberFormatException e) {
            unimplemented();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        checkDownloadProblems();
    }

}