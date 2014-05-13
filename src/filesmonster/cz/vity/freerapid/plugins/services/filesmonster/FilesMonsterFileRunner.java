package cz.vity.freerapid.plugins.services.filesmonster;

import cz.vity.freerapid.plugins.container.FileInfo;
import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Lukiz
 * @author ntoskrnl
 * @author Stan
 */
class FilesMonsterFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilesMonsterFileRunner.class.getName());

    private static final String AJAX_HEADER_FIELD = "X-Requested-With";
    private static final String AJAX_HEADER_VALUE = "XMLHttpRequest";

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkContentOrConnectionProblems();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        boolean partOfFile = fileURL.contains("free/2");
        if (!partOfFile) {
            PlugUtils.checkName(httpFile, content, "File name:</td>\n<td>", "</td>");
            PlugUtils.checkFileSize(httpFile, content, "File size:</td>\n<td>", "</td>");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        if (!content.contains("slowdownload") && !partOfFile) {
            throw new NotRecoverableDownloadException("Only Premium download available");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (!makeRedirectedRequest(method)) { //we make the main request
            checkContentOrConnectionProblems();
        }
        if (!fileURL.contains("free/2")) { // main file
            processMainFile();
        } else { // part of main file
            processPartOfFile();
        }
    }

    private void processMainFile() throws ErrorDuringDownloadingException, IOException {
        checkProblems();//check problems
        checkNameAndSize(getContentAsString());//extract file name and size from the page

        final HttpMethod postMethodForTicket = getMethodBuilder()
                .setReferer(fileURL).setActionFromFormByName("slowdownload", true).toPostMethod();
        if (!makeRedirectedRequest(postMethodForTicket)) {
            checkContentOrConnectionProblems();
        }
        String reserveTicket;
        try {
            reserveTicket = PlugUtils.getStringBetween(getContentAsString(), "rftUrl = '", "';");
        } catch (Exception e) {
            reserveTicket = PlugUtils.getStringBetween(getContentAsString(), "reserve_ticket('", "');");
        }

        final String referer = "http://filesmonster.com" + postMethodForTicket.getPath();
        final String dlPrefix = postMethodForTicket.getPath()
                .substring(0, postMethodForTicket.getPath().indexOf("free/") + "free".length())
                + "/";

        final HttpMethod getMethodForFiles = getMethodBuilder()
                .setAction(reserveTicket).setReferer(referer).toGetMethod();
        getMethodForFiles.addRequestHeader(AJAX_HEADER_FIELD, AJAX_HEADER_VALUE); // send as AJAX
        if (!makeRequest(getMethodForFiles)) {
            checkContentOrConnectionProblems();
        }
        List<FileInfo> files = getFilesFromJson(
                getContentAsString(), "http://filesmonster.com" + dlPrefix, referer);
        getPluginService().getPluginContext().getQueueSupport()
                .addLinksToQueueFromContainer(httpFile, files);
    }

    private void processPartOfFile() throws Exception {
        checkProblems();//check problems
        checkNameAndSize(getContentAsString());//extract file name and size from the page

        //       final HttpMethod getMethodForCaptcha = getMethodBuilder()
        //               .setAction(fileURL).setReferer(httpFile.getDescription()).toGetMethod();
        //       getMethodForCaptcha.addRequestHeader(AJAX_HEADER_FIELD, AJAX_HEADER_VALUE); // send as AJAX
        //       if (!makeRedirectedRequest(getMethodForCaptcha)) {
        //           checkContentOrConnectionProblems();
        //       }
        while (getContentAsString().contains("recaptcha")) {
            if (!makeRedirectedRequest(stepCaptcha(fileURL))) {
                checkContentOrConnectionProblems();
            }
        }
        if (!getContentAsString().contains("get_link('")) {
            checkContentOrConnectionProblems();
        }

        final HttpMethod getMethodForLink = getMethodBuilder()
                .setAction(PlugUtils.getStringBetween(getContentAsString(), "get_link('", "');"))
                .setReferer(fileURL).toGetMethod();
        getMethodForLink.addRequestHeader(AJAX_HEADER_FIELD, AJAX_HEADER_VALUE); // send as AJAX
        if (!makeRequest(getMethodForLink)) checkContentOrConnectionProblems();
        if (!getContentAsString().contains("url\":\"")) checkJSONProblems(getContentAsString());
        String finalLink = PlugUtils.getStringBetween(getContentAsString(),
                "url\":\"", "\"").replace("\\", "");
        //  String fileRequest = PlugUtils.getStringBetween(getContentAsString(),
        //          "file_request\":\"", "\"");
        httpFile.setFileName(finalLink.substring(1 + finalLink.lastIndexOf("/")));

        final HttpMethod postMethodForDownload = getMethodBuilder()
                .setAction(finalLink).setReferer(fileURL)
                        //          .setParameter("X-File-Request", fileRequest)
                .setParameter("x", "688").setParameter("y", "258").toPostMethod(); // mouse position when clicked to download button - not necessary but what if...
        postMethodForDownload.addRequestHeader(AJAX_HEADER_FIELD, AJAX_HEADER_VALUE); // send as AJAX
        if (!tryDownloadAndSaveFile(postMethodForDownload)) {
            checkJSONProblems(getContentAsString());
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
        }
    }

    private List<FileInfo> getFilesFromJson(String json, String urlPrefix, String referer) throws MalformedURLException {
        List<FileInfo> files = new ArrayList<FileInfo>();
        for (int i = 1; true; i++) {
            try {
                String fileString = PlugUtils.getStringBetween(json, "{", "}", i);
                FileInfo fileInfo = new FileInfo(new URL(urlPrefix + 2 + "/" +
                        PlugUtils.getStringBetween(fileString, "dlcode\":\"", "\"")));
                fileInfo.setFileName(PlugUtils.getStringBetween(fileString, "name\":\"", "\""));
                try {
                    fileInfo.setFileSize(PlugUtils.getNumberBetween(fileString, "size\":\"", "\""));
                } catch (Exception e) {
                    fileInfo.setFileSize(PlugUtils.getNumberBetween(fileString, "size\":", ","));
                }
                fileInfo.setDescription(referer);
                files.add(fileInfo);
            } catch (PluginImplementationException e) {
                break;
            }
        }
        return files;
    }

    private void checkContentOrConnectionProblems() throws ErrorDuringDownloadingException {
        checkProblems();
        throw new ServiceConnectionProblemException();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("File not found") || contentAsString.contains("File was deleted by owner") || contentAsString.contains("This document does not exist"))
            throw new URLNotAvailableAnymoreException("File not found");

        if (contentAsString.contains("You can wait for the start of downloading"))
            throw new YouHaveToWaitException("You have got max allowed download sessions from the same IP", PlugUtils.getWaitTimeBetween(contentAsString, " start of downloading", " minute", TimeUnit.MINUTES));

        if (contentAsString.contains("There are no free download slots available"))
            throw new ServiceConnectionProblemException("No more free download slots");

        if (contentAsString.contains("Next download will be available in")) {
            final int time = PlugUtils.getWaitTimeBetween(contentAsString,
                    "Next download will be available in", "min", TimeUnit.MINUTES);
            throw new YouHaveToWaitException("Waiting for next file", time);
        }
        if (contentAsString.contains("will be available for download in")) {
            final int time = PlugUtils.getWaitTimeBetween(contentAsString,
                    "will be available for download in", "min", TimeUnit.MINUTES);
            throw new YouHaveToWaitException("Waiting for next file", time);
        }
    }

    private void checkJSONProblems(final String content) throws ErrorDuringDownloadingException {
        final String error;
        try {
            error = PlugUtils.getStringBetween(content, "\"error\":\"", "\"");
        } catch (PluginImplementationException e) {
            return;//no error
        }
        if (error == null || error.isEmpty()) {
            return;
        }
        if (error.contains("404")) {
            throw new ServiceConnectionProblemException("AJAX server response: There is no such file");
        }
        if (error.contains("500")) {
            throw new ServiceConnectionProblemException("AJAX server response: Internal error");
        }
        if (error.contains("Your IP address has been changed")) {
            throw new NotRecoverableDownloadException("Link expired - Start main file download again for new link to this part");
        }
        if (error.contains("downloaded file recently")) {
            throw new NotRecoverableDownloadException("Link probably expired - Try restart main download file for new link to this part");
        }
        if (error.contains("Link has expired")) {
            throw new NotRecoverableDownloadException("Link has expired. Please, generate it once more.");
        }
        throw new PluginImplementationException("Unspecified server response");
    }

    private HttpMethod stepCaptcha(final String referer) throws Exception {
        final Matcher m = getMatcherAgainstContent("api.recaptcha.net/noscript\\?k=([^\"]+)\"");
        if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");
        final String reCaptchaKey = m.group(1);

        final String content = getContentAsString();
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final CaptchaSupport captchaSupport = getCaptchaSupport();

        final String captchaURL = r.getImageURL();
        logger.info("Captcha URL " + captchaURL);

        final String captcha = captchaSupport.getCaptcha(captchaURL);
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        r.setRecognized(captcha);

        return r.modifyResponseMethod(getMethodBuilder(content)
                .setAction(referer).setReferer(referer)).toPostMethod(); // action is same as referer
    }
}