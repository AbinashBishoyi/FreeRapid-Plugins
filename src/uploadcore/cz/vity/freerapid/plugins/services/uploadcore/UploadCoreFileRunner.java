package cz.vity.freerapid.plugins.services.uploadcore;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class UploadCoreFileRunner extends XFileSharingRunner {

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("This Ticket Will be Active For Your Computer");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "checked == false\\) \\{\\s+document\\.getElementById\\(\"dl\"\\)\\.innerHTML=\"<a href\\s?=\\s?(?:\"|')(http.+?" + Pattern.quote(httpFile.getFileName()) + ")(?:\"|')");
        return downloadLinkRegexes;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new UploadCoreFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    public void run() throws Exception {
        //super.run();
        setLanguageCookie();
        //logger.info("Starting download in TASK " + fileURL);
        login();
        HttpMethod method = getGetMethod(fileURL);
        int httpStatus = client.makeRequest(method, true);          // ## Changed to true & plugin works again
        if (httpStatus / 100 == 3) {
            handleDirectDownload(method);
            return;
        }
        if (httpStatus != 200) {
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
            final MethodBuilder methodBuilder = getXFSMethodBuilder();
            final int waitTime = getWaitTime();
            final long startTime = System.currentTimeMillis();
            stepPassword(methodBuilder);
            if (!stepCaptcha(methodBuilder)) {                // skip the wait timer if its on the same page
                sleepWaitTime(waitTime, startTime);           //   as a captcha of type ReCaptcha
            }
            method = methodBuilder.toPostMethod();
            httpStatus = client.makeRequest(method, false);
            if (httpStatus / 100 == 3) {
                //redirect to download file location
                method = stepRedirectToFileLocation(method);
                break;
            } else if (checkDownloadPageMarker()) {
                //page containing download link
                final String downloadLink = getDownloadLinkFromRegexes();
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(downloadLink)
                        .toGetMethod();
                break;
            }
            checkDownloadProblems();
        }
        doDownload(method);
    }
}