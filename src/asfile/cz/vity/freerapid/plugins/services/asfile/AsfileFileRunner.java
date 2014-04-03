package cz.vity.freerapid.plugins.services.asfile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;


/**
 * Class which contains main code
 *
 * @author RickCL
 */
class AsfileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AsfileFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkUrlUnavailable(getMethod);
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "name: '", "'");
        PlugUtils.checkFileSize(httpFile, content, "size:", "</");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }


    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);
        if (!makeRedirectedRequest(getMethod)) {
            checkUrlUnavailable(getMethod);
            checkNameAndSize(getContentAsString());
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        int waitingTime = getWaitingTime();
        //they have weird waiting time handling
        if (waitingTime > (5 * 60)) {
            //90 minutes waiting time
            //downloadTask.sleep(waitingTime + 1);
            throw new YouHaveToWaitException("You have to wait " + waitingTime + 1 + " seconds", waitingTime + 1);
        } else {
            //downloadTask.sleep(waitingTime + 1);  //skippable
        }
        while (!getContentAsString().contains("redirected to download page")) {
            stepCaptcha();
        }

        //second waiting time
        waitingTime = getWaitingTime();
        downloadTask.sleep(waitingTime + 1);
        HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromAHrefWhereATagContains("Slow download")
                .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        final String referer = httpMethod.getURI().toString();

        checkProblems();
        final String hash = PlugUtils.getStringBetween(getContentAsString(), "hash: '", "'");
        final String path = PlugUtils.getStringBetween(getContentAsString(), "path: '", "'");
        final String storage = PlugUtils.getStringBetween(getContentAsString(), "storage: '", "'");
        final String name = PlugUtils.getStringBetween(getContentAsString(), "name: '", "'");
        final String convertHashToLinkAction = PlugUtils.getStringBetween(getContentAsString(), "$.post('", "'");
        //third waiting time
        waitingTime = getWaitingTime();
        downloadTask.sleep(waitingTime + 1);
        httpMethod = getMethodBuilder()
                .setReferer(referer)
                .setAction(convertHashToLinkAction)
                .setParameter("hash", hash)
                .setParameter("path", path)
                .setParameter("storage", storage)
                .setParameter("name", name)
                .setAjax()
                .toPostMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        final String downloadURL = PlugUtils.getStringBetween(getContentAsString(), "{\"url\":\"", "\"").replaceAll("\\\\/", "/");
        httpMethod = getMethodBuilder()
                .setReferer(referer)
                .setAction(downloadURL)
                .toGetMethod();
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download:" + downloadURL);
        }
    }

    private int getWaitingTime() throws PluginImplementationException {
        return PlugUtils.getNumberBetween(getContentAsString(), "var timer =", ";");
    }

    private void stepCaptcha() throws Exception {
        final MethodBuilder methodBuilder = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("form_cap", true)
                .setAction(fileURL + "#top");
        final String publicKey = PlugUtils.getStringBetween(getContentAsString(), "/api/challenge?k=", "\"");
        final ReCaptcha reCaptcha = new ReCaptcha(publicKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(reCaptcha.getImageURL());
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        reCaptcha.setRecognized(captcha);
        if (!makeRedirectedRequest(reCaptcha.modifyResponseMethod(methodBuilder).toPostMethod())) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
    }

    private void checkUrlUnavailable(final HttpMethod method) throws Exception {
        if (method.getURI().toString().contains("/file_is_unavailable/")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    public void checkProblems() throws ErrorDuringDownloadingException {
        if (fileURL.contains("/file_is_unavailable/")
                || getContentAsString().contains("Delete Reason")
                || getContentAsString().contains("File is deleted")
                || getContentAsString().contains("File is not exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("available only to premium")) {
            throw new PluginImplementationException("This file is available only to premium users");
        }
    }

}