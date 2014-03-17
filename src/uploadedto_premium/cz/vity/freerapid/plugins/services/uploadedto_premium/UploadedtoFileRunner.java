package cz.vity.freerapid.plugins.services.uploadedto_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class UploadedtoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadedtoFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        checkUrl();
        super.runCheck();
        addCookie(new Cookie(".uploaded.net", "lang", "en", "/", 86400, false));
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        if (fileURL.contains("/f/") || fileURL.contains("/folder/")) {
            httpFile.setFileName("Folder : " + PlugUtils.getStringBetween(getContentAsString(), "<title>", "</title>"));
            httpFile.setFileSize(PlugUtils.getNumberBetween(getContentAsString(), ">(", ")<"));
        } else {
            final Matcher matcher = getMatcherAgainstContent("<title>(.+?) \\(([^\\(\\)]+?)\\) \\- uploaded\\.(?:to|net)</title>");
            if (!matcher.find()) {
                throw new PluginImplementationException("File name/size not found");
            }
            httpFile.setFileName(PlugUtils.unescapeHtml(matcher.group(1)));
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2).replace(".", "")));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkUrl() {
        if (fileURL.contains("uploaded.to/")) {
            fileURL = fileURL.replaceFirst("uploaded.to/", "uploaded.net/");
        }
        if (fileURL.contains("ul.to/")) {
            fileURL = fileURL.replaceFirst("ul.to/", "uploaded.net/file/");
        }
    }

    @Override
    public void run() throws Exception {
        checkUrl();
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".uploaded.net", "lang", "en", "/", 86400, false));
        login();
        HttpMethod method = getGetMethod(fileURL);
        if (fileURL.contains("/f/") || fileURL.contains("/folder/")) {
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            List<URI> list = new LinkedList<URI>();
            final Matcher m = PlugUtils.matcher("<h2><a href=\"(.+?)/from/", getContentAsString());
            while (m.find()) {
                list.add(new URI("http://uploaded.net/" + m.group(1).trim()));
            }
            if (list.isEmpty()) throw new PluginImplementationException("No links found");
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
            httpFile.setFileName("Link(s) Extracted !");
            httpFile.setState(DownloadState.COMPLETED);
            httpFile.getProperties().put("removeCompleted", true);
            return;
        }

        int httpStatus = client.makeRequest(method, false);
        if (httpStatus / 100 == 3) {    // direct download
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else if (httpStatus == 200) {
            checkProblems();
            checkNameAndSize();
            method = getMethodBuilder().setActionFromFormWhereTagContains("Premium Download", true).toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Page not found") || getContentAsString().contains("The requested file isn't available anymore")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("Our service is currently unavailable in your country")) {
            throw new NotRecoverableDownloadException("Our service is currently unavailable in your country");
        }
    }

    private void login() throws Exception {
        synchronized (UploadedtoFileRunner.class) {
            UploadedtoServiceImpl service = (UploadedtoServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No Uploaded account login information!");
                }
            }
            final HttpMethod method = getMethodBuilder()
                    .setAction("http://uploaded.net/io/login")
                    .setParameter("id", pa.getUsername())
                    .setParameter("pw", pa.getPassword())
                    .toPostMethod();
            method.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            setFileStreamContentTypes(new String[0], new String[]{"application/javascript"});
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            if (getContentAsString().contains("err")) {
                throw new BadLoginException("Invalid Uploaded account login information!");
            }
        }
    }

}