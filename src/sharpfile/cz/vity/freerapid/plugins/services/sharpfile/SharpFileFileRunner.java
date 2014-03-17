package cz.vity.freerapid.plugins.services.sharpfile;

import cz.vity.freerapid.plugins.exceptions.*;
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
class SharpFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SharpFileFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<title>Download", "</title>");
        // no file size listed
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            String contentAsString = getContentAsString();//check for response
            checkFileProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            checkDownloadProblems();

            MethodBuilder methodBuilder = getMethodBuilder()
                    .setReferer("")
                    .setActionFromFormWhereTagContains("free", true)
                    .removeParameter("prem")
                    .setAction(fileURL);
            if (!makeRedirectedRequest(methodBuilder.toPostMethod())) {
                checkDownloadProblems();
                throw new ServiceConnectionProblemException("Error starting download-1");
            }
            checkDownloadProblems();

            boolean bReCaptcha = true;
            while (bReCaptcha) {
                bReCaptcha = false;
                /*  contentAsString = getContentAsString();
            Matcher match = PlugUtils.matcher("var x = (\\d+)", contentAsString);
            if (!match.find()) {
                throw new PluginImplementationException("Wait time not found");
            }
            Integer iWait = Integer.parseInt(match.group(1));
            downloadTask.sleep(iWait + 1);  */

                methodBuilder = getMethodBuilder()
                        .setReferer(fileURL)
                        .setParameter("download_now", "Create Download Link")
                        .setParameter("referer", "")
                        .setAction(fileURL);
                stepReCaptcha(methodBuilder);

                if (!makeRedirectedRequest(methodBuilder.toPostMethod())) {
                    checkDownloadProblems();
                    throw new ServiceConnectionProblemException("Error starting download-2");
                }
                checkDownloadProblems();

                if (getContentAsString().contains("Invalid Captcha, try again"))
                    bReCaptcha = true;
            }

            HttpMethod hMethod = methodBuilder.setActionFromAHrefWhereATagContains(httpFile.getFileName()).toGetMethod();
            if (!tryDownloadAndSaveFile(hMethod)) {
                checkDownloadProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
    }


    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("The file you requested does not exist")
                || content.contains("not found on this server")
                || content.contains("404 Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("You have exceeded your download limit")) {
            final Matcher matcher = getMatcherAgainstContent("(\\d+) minutes?");
            int waitMinutes = 1;
            if (matcher.find()) {
                waitMinutes = Integer.parseInt(matcher.group(1));
            }
            final int waitTime = waitMinutes * 60;
            throw new YouHaveToWaitException("You have to wait " + matcher.group(), waitTime);
        }
        if (content.contains("till next download") || content.contains("You have to wait")) {
            final Matcher matcher = getMatcherAgainstContent("(?:(\\d+) hours?, )?(?:(\\d+) minutes?, )?(?:(\\d+) seconds?)");
            int waitHours = 0, waitMinutes = 0, waitSeconds = 0;
            if (matcher.find()) {
                if (matcher.group(1) != null) {
                    waitHours = Integer.parseInt(matcher.group(1));
                }
                if (matcher.group(2) != null) {
                    waitMinutes = Integer.parseInt(matcher.group(2));
                }
                waitSeconds = Integer.parseInt(matcher.group(3));
            }
            final int waitTime = (waitHours * 60 * 60) + (waitMinutes * 60) + waitSeconds;
            throw new YouHaveToWaitException("You have to wait " + matcher.group(), waitTime);
        }
    }

    private void stepReCaptcha(MethodBuilder methodBuilder) throws Exception {
        Matcher match = getMatcherAgainstContent("papi/challenge\\.noscript\\?k=([^\"]+)\"");
        if (!match.find()) {
            throw new PluginImplementationException("Captcha not found");
        }
        final String captchaKey = match.group(1);

        String mediaType;
        do {
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL).setAction("https://api-secure.solvemedia.com/papi/_challenge.js")
                    .setParameter("k", captchaKey + ";f=_ACPuzzleUtil.callbacks%5B0%5D;l=en;t=img;s=standard;c=js,swf11,swf11.2,swf,h5c,h5ct,svg,h5v,v/h264,v/ogg,v/webm,h5a,a/mp3,a/ogg,ua/chrome,ua/chrome18,os/nt,os/nt6.0,fwv/htyg64,jslib/jquery,jslib/jqueryui;ts=1339103245;th=custom;r=" + Math.random())
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                throw new ServiceConnectionProblemException();
            }
            final Matcher mediaTypeMatcher = getMatcherAgainstContent("\"mediatype\"\\s*:\\s*\"(.+?)\",");
            if (!mediaTypeMatcher.find()) {
                throw new PluginImplementationException("Captcha media type not found");
            }
            mediaType = mediaTypeMatcher.group(1);
        } while (!mediaType.equals("img"));

        match = getMatcherAgainstContent("\"chid\"\\s*:\\s*\"(.+?)\",");
        if (!match.find()) throw new PluginImplementationException("Captcha ID not found");
        final String captchaChID = match.group(1);
        final String captchaImg = "https://api-secure.solvemedia.com/papi/media?c=" + captchaChID + ";w=300;h=150;fg=333333;bg=ffffff";

        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaTxt = captchaSupport.getCaptcha(captchaImg);
        if (captchaTxt == null) throw new CaptchaEntryInputMismatchException("No Input");

        methodBuilder.setParameter("adcopy_challenge", captchaChID);
        methodBuilder.setParameter("adcopy_response", captchaTxt);
    }
}