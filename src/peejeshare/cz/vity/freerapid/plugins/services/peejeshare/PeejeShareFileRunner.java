package cz.vity.freerapid.plugins.services.peejeshare;

import cz.vity.freerapid.plugins.exceptions.*;
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
 * @author tong2shot
 */
class PeejeShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PeejeShareFileRunner.class.getName());
    private final static String SERVICE_TITLE = "PeejeShare";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        if (fileURL.contains("http://peejeshare")) {
            fileURL = fileURL.replaceFirst("http://peejeshare", "http://www.peejeshare");
        }
        if (!isPassworded()) {
            final String regexp = "File information:<b> (.+?) - (.+?) </b>";
            final Matcher matcher = getMatcherAgainstContent(regexp);
            if (!matcher.find()) {
                throw new PluginImplementationException("File name not found");
            }
            httpFile.setFileName(matcher.group(1));
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();

            HttpMethod httpMethod;
            String password = "";
            if (isPassworded()) {
                do {
                    password = getDialogSupport().askForPassword(SERVICE_TITLE);
                    if (password == null) {
                        throw new NotRecoverableDownloadException("This file is secured with a password");
                    }
                    httpMethod = getMethodBuilder()
                            .setReferer(fileURL)
                            .setActionFromFormByName("pswcheck", true)
                            .setAction(fileURL)
                            .setParameter("psw", password)
                            .toPostMethod();
                    if (!makeRedirectedRequest(httpMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                } while (getContentAsString().contains("Invalid Password"));
            }

            checkNameAndSize(contentAsString);
            final MethodBuilder methodBuilder = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("Create Download Link", true)
                    .setAction(fileURL)
                    .setParameter("psw", password);
            httpMethod = methodBuilder.toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("Click here to Download")
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private boolean isPassworded() {
        return getContentAsString().contains("file is password-protected");
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file you requested does not exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("download slots for this file are currently filled")) {
            throw new YouHaveToWaitException("All download slots for this file are currently filled", 120);
        }
    }

}