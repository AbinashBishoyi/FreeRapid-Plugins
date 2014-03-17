package cz.vity.freerapid.plugins.services.dizzcloud;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
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
class DizzCloudFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DizzCloudFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        Matcher match = PlugUtils.matcher("file-name\">(.+?)\\[(.+?)\\]</div>", content);
        if (!match.find()) {
            match = PlugUtils.matcher("File: </div>(.+?)<span.*?>\\[(.+?)\\]<span>", content);
            if (!match.find()) {
                match = PlugUtils.matcher("<div class=\"name\">(.+?)</div>\\s+?<div id=\"file-size\">(.+?)</div>", content);
                if (!match.find())
                    throw new PluginImplementationException("File name/size not found");
            }
        }
        httpFile.setFileName(match.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(2)));
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

            do {
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                final HttpMethod cMethod = doCaptcha(getMethodBuilder()
                        .setAction(PlugUtils.getStringBetween(contentAsString, "getJSON(\"", "\","))
                        .setReferer(fileURL)
                        .setAjax()
                ).toGetMethod();
                cMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
                if (!makeRedirectedRequest(cMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            } while (!getContentAsString().contains("\"ok\":true"));
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(PlugUtils.getStringBetween(getContentAsString(), "href\":\"", "\"").replace("\\", ""))
                    .toHttpMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        final String content = getContentAsString();
        if (content.contains("The file's owner has disabled the ability to free download a file larger than") ||
                content.contains("File owner has disabled<br/>free download for files")) {
            throw new NotRecoverableDownloadException("Download of this size disabled for free users by owner");
        }
        if (content.contains("Next free download from your ip will be available in")) {
            final String waitStr = PlugUtils.getStringBetween(content, "Next free download from your ip will be available in <b>", "</p>");
            Integer waitTime = Integer.parseInt(waitStr.split(" ")[0]);
            if (waitStr.contains("minute")) waitTime = waitTime * 60;
            throw new YouHaveToWaitException("You need to wait", waitTime);
        }
    }

    private MethodBuilder doCaptcha(MethodBuilder methodBuilder) throws Exception {
        final String reCaptchaKey = PlugUtils.getStringBetween(getContentAsString(), "recaptcha/api/challenge?k=", "\"");
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);
        methodBuilder.setParameter("type", "recaptcha");
        methodBuilder.setParameter("challenge", r.getChallenge());
        methodBuilder.setParameter("capture", captcha);
        return methodBuilder;
    }
}