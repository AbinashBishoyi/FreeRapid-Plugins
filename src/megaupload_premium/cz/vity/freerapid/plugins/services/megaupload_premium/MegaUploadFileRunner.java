package cz.vity.freerapid.plugins.services.megaupload_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MegaUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MegaUploadFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL();
        addCookie(new Cookie(".megaupload.com", "l", "en", "/", 86400, false));
        addCookie(new Cookie(".megaporn.com", "l", "en", "/", 86400, false));
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (!isFolder()) checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkURL();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".megaupload.com", "l", "en", "/", 86400, false));
        addCookie(new Cookie(".megaporn.com", "l", "en", "/", 86400, false));

        login();

        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();

            if (isFolder()) {
                stepFolder();
                return;
            }

            checkNameAndSize();

            if (getContentAsString().contains("download is password protected")) {
                stepPasswordPage();
            }

            final Matcher matcher = getMatcherAgainstContent("\"(http://www\\d+?\\.mega(?:upload|porn)\\.com/files/[^\"]+?)\"");
            if (!matcher.find()) {
                if (makeRedirectedRequest(getGetMethod("/?c=account"))) {
                    if (getContentAsString().contains("class=\"account_txt\">(Regular)")) {
                        throw new NotRecoverableDownloadException("Account is not premium!");
                    }
                }
                throw new PluginImplementationException("Download link not found");
            }
            final String url = matcher.group(1);

            final int index = url.lastIndexOf('/');
            if (index > 0) {
                final String name = url.substring(index + 1);
                httpFile.setFileName(PlugUtils.unescapeHtml(URLDecoder.decode(name, "UTF-8")));
            }

            method = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<div class=\"download_file_name\">", "</div>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<div class=\"download_file_size\">", "</div>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("the link you have clicked is not available") || content.contains("<h1>Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("trying to access is temporarily unavailable")) {
            throw new ServiceConnectionProblemException("The file you are trying to access is temporarily unavailable");
        }
        if (content.contains("We have detected an elevated number of requests")) {
            throw new ServiceConnectionProblemException("We have detected an elevated number of requests");
        }
    }

    private void login() throws Exception {
        synchronized (MegaUploadFileRunner.class) {
            final MegaUploadServiceImpl service = (MegaUploadServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No MegaUpload Premium account login information!");
                }
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction("/?c=login")
                    .setParameter("login", "1")
                    .setParameter("username", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");

            if (getContentAsString().contains("Username and password do not match"))
                throw new BadLoginException("Invalid MegaUpload Premium account login information!");
        }
    }

    private void checkURL() {
        final String host = httpFile.getFileUrl().getHost();
        if (host.contains("megarotic") || host.contains("sexuploader") || host.contains("megaporn")) {
            fileURL = fileURL.replace("megarotic.com", "megaporn.com").replace("sexuploader.com", "megaporn.com");
        }
    }

    private void stepPasswordPage() throws Exception {
        while (getContentAsString().contains("Please enter the password")) {
            final String password = getDialogSupport().askForPassword("MegaUpload");
            if (password == null) {
                throw new NotRecoverableDownloadException("This file is secured with a password");
            }
            final HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(fileURL)
                    .setParameter("filepassword", password)
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private boolean isFolder() {
        return PlugUtils.find("[\\?&]f=", fileURL);
    }

    private void stepFolder() throws Exception {
        final List<URI> list = new LinkedList<URI>();
        for (int page = 1; ; page++) {
            final String url = fileURL + "&ajax=1&pa=" + page + "&so=name&di=asc&rnd=" + System.currentTimeMillis();
            final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final int total = getTotal();
            final int previousSize = list.size();
            final Matcher matcher = getMatcherAgainstContent("\"url\":\"(.+?)\"");
            while (matcher.find()) {
                try {
                    list.add(new URI(matcher.group(1).replace("\\/", "/")));
                } catch (final URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
            if (list.size() >= total || list.size() <= previousSize) {
                break;
            }
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.getProperties().put("removeCompleted", true);
    }

    private int getTotal() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("\"total\":\"(\\d+)\"");
        if (!matcher.find()) {
            throw new PluginImplementationException("Total number of links not found");
        }
        return Integer.parseInt(matcher.group(1));
    }

}