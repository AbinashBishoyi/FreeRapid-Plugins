package cz.vity.freerapid.plugins.services.filemonkey;

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
 * @author birchie
 */
class FileMonkeyFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileMonkeyFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkFileProblems();
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h1>Download", "</h1>");
        PlugUtils.checkFileSize(httpFile, content, "<div class='badge pull-right'>", "</div>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkFileProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL).setAction(fileURL)
                    .setBaseURL("http://www.filemonkey.in")
                    .setParameter("action", "freedownload")
                    .toPostMethod();
            //final int wait = PlugUtils.getNumberBetween(contentAsString, "var waitsecs = ", ";");
            //downloadTask.sleep(wait + 1);
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            httpMethod = doCaptcha(getMethodBuilder()
                    .setActionFromFormWhereTagContains("captcha", true)
                    .setReferer(fileURL).setAction(fileURL)
                    .setBaseURL("http://www.filemonkey.in")
            ).toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setBaseURL("http://www.filemonkey.in")
                    .setActionFromAHrefWhereATagContains("Download now")
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkFileProblems();
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("This file has not been found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("You have already downloaded a file within the last")) {
            final Matcher match = PlugUtils.matcher("Please wait\\s*?(.+?)or get premium", content);
            if (!match.find()) throw new YouHaveToWaitException("Please wait before next download", 500);
            int waitTime = 0;
            if (match.group(1).contains("minute")) {
                final Matcher matchMin = PlugUtils.matcher("(\\d+?) minute", match.group(1));
                if (!matchMin.find()) throw new YouHaveToWaitException("Please wait before next download", 500);
                waitTime += 60 * Integer.parseInt(matchMin.group(1));
            }
            if (match.group(1).contains("second")) {
                final Matcher matchSec = PlugUtils.matcher("(\\d+?) second", match.group(1));
                if (!matchSec.find()) throw new YouHaveToWaitException("Please wait before next download", 500);
                waitTime += Integer.parseInt(matchSec.group(1));
            }
            throw new YouHaveToWaitException("Please wait " + match.group(1), waitTime);
        }
    }

    private MethodBuilder doCaptcha(MethodBuilder builder) throws Exception {
        final Matcher m = getMatcherAgainstContent("challenge\\.(?:no)?script\\?k=(.+?)\"");
        if (!m.find()) throw new PluginImplementationException("Captcha key not found");
        final String captchaKey = m.group(1);
        final SolveMediaCaptcha solveMediaCaptcha = new SolveMediaCaptcha(captchaKey, client, getCaptchaSupport(), true);
        solveMediaCaptcha.askForCaptcha();
        solveMediaCaptcha.modifyResponseMethod(builder);
        return builder;
    }
}