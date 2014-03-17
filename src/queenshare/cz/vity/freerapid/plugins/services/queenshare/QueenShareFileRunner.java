package cz.vity.freerapid.plugins.services.queenshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class QueenShareFileRunner extends XFileSharingRunner {

    @Override
    public void run() throws Exception {
        //   super.run();
        setLanguageCookie();
        //  logger.info("Starting download in TASK " + fileURL);
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
            String contentAsString = getContentAsString();
            MethodBuilder methodBuilder;
            if (contentAsString.contains("fname")) {      // XFS found this section
                methodBuilder = getMethodBuilder()
                        .setReferer(fileURL).setAction(fileURL)
                        .setParameter("op", PlugUtils.getStringBetween(contentAsString, "name=\"op\" value=\"", "\">"))
                        .setParameter("id", PlugUtils.getStringBetween(contentAsString, "name=\"id\" value=\"", "\">"))
                        .setParameter("fname", PlugUtils.getStringBetween(contentAsString, "name=\"fname\" value=\"", "\">"))
                        .setParameter("method_free", PlugUtils.getStringBetween(contentAsString, "name=\"method_free\" value=\"", "\">"))
                        .setParameter("method_premium", "");
            } else {
                methodBuilder = getMethodBuilder()        // XFS DID NOT found this section
                        .setReferer(fileURL).setAction(fileURL)
                        .setParameter("op", PlugUtils.getStringBetween(contentAsString, "name=\"op\" value=\"", "\">"))
                        .setParameter("id", PlugUtils.getStringBetween(contentAsString, "name=\"id\" value=\"", "\">"))
                        .setParameter("rand", PlugUtils.getStringBetween(contentAsString, "name=\"rand\" value=\"", "\">"))
                        .setParameter("method_free", PlugUtils.getStringBetween(contentAsString, "name=\"method_free\" value=\"", "\">"))
                        .setParameter("method_premium", "")
                        .setParameter("down_direct", PlugUtils.getStringBetween(contentAsString, "name=\"down_direct\" value=\"", "\">"));
            }
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


    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("alt=\"Get File\"");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("<!-- <a href\\s*=\\s*\"(.*)\">");
        return downloadLinkRegexes;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new QueenShareFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<h2>File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("You can download files up to")) {
            throw new NotRecoverableDownloadException(PlugUtils.getStringBetween(contentAsString, "<p class=\"err\">", "<br>"));
        }
        super.checkDownloadProblems();
    }
}