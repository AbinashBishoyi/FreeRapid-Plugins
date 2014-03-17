package cz.vity.freerapid.plugins.services.fiberupload;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FiberUploadFileRunner extends XFileSharingRunner {
    private final static Logger logger = Logger.getLogger(FiberUploadFileRunner.class.getName());

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new FiberUploadFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new FiberUploadFileNameHandler());
        return fileNameHandlers;
    }

    @Override
    public void runCheck() throws Exception {
        if (fileURL.matches("http://(?:www\\.)?bulletupload\\.com/.+")) {
            httpFile.setNewURL(new URL(fileURL.replaceFirst("bulletupload\\.com", "fiberupload.com")));
        }
        super.runCheck();
    }

    @Override
    public void run() throws Exception {
        setLanguageCookie();
        logger.info("Starting download in TASK " + fileURL);
        login();
        HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        checkFileProblems();
        checkNameAndSize();
        checkDownloadProblems();
        for (int loopCounter = 0; ; loopCounter++) {
            if (loopCounter >= 8) {
                //avoid infinite loops
                throw new PluginImplementationException("Cannot proceed to download link");
            }
            final MethodBuilder methodBuilder = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("method_free", true)
                    .setAction(fileURL);
            if (!methodBuilder.getParameters().get("method_free").isEmpty()) {
                methodBuilder.removeParameter("method_premium");
            }
            final int waitTime = getWaitTime();
            final long startTime = System.currentTimeMillis();
            stepPassword(methodBuilder);
            if (!stepCaptcha(methodBuilder)) {                // skip the wait timer if its on the same page
                sleepWaitTime(waitTime, startTime);           //   as a captcha of type ReCaptcha
            }
            method = methodBuilder.toPostMethod();
            final int httpStatus = client.makeRequest(method, false);
            if (httpStatus / 100 == 3) {
                //redirect to download file location
                final Header locationHeader = method.getResponseHeader("Location");
                if (locationHeader == null) {
                    throw new PluginImplementationException("Invalid redirect");
                }
                //this code is slightly different from super.run(), that's why we need to override it.
                final String downloadFileURL = locationHeader.getValue();
                logger.info("download file URL : " + downloadFileURL);
                method = getMethodBuilder()
                        .setReferer(downloadFileURL)
                        .setAction(downloadFileURL.replaceAll(httpFile.getFileName(), "GO/" + httpFile.getFileName()))
                        .toGetMethod();
                break;
            } else if (getContentAsString().contains("File Download Link Generated")
                    || getContentAsString().contains("This direct link will be ")) {
                //page containing download link
                final Matcher matcher = getMatcherAgainstContent("<a href=\"(http.+?" + Pattern.quote(httpFile.getFileName()) + ")\"");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Download link not found");
                }
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(matcher.group(1))
                        .toGetMethod();
                break;
            }
            checkDownloadProblems();
        }
        downloadTask.sleep(5);
        setFileStreamContentTypes("text/plain");
        if (!tryDownloadAndSaveFile(method)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }
}