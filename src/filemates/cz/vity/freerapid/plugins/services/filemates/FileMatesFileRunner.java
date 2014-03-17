package cz.vity.freerapid.plugins.services.filemates;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FileMatesFileRunner extends XFileSharingRunner {

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("Click here to start the download with low speed...");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("onclick=\"downLinkDo\\('(http.+?" + Pattern.quote(httpFile.getFileName()) + ")','");
        return downloadLinkRegexes;
    }

    @Override
    public void run() throws Exception {
        super.run();
        setLanguageCookie();
        //    logger.info("Starting download in TASK " + fileURL);
        if (login()) {
            HttpMethod method = getGetMethod(fileURL);
            setFileStreamContentTypes("text/plain");
            if (!tryDownloadAndSaveFile(method)) {
                checkDownloadProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
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
                final MethodBuilder methodBuilder = getXFSMethodBuilder();
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
            setFileStreamContentTypes("text/plain");
            if (!tryDownloadAndSaveFile(method)) {
                checkDownloadProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        }
    }

}