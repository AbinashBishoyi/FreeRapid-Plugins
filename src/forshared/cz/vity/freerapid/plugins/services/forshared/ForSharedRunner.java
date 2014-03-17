package cz.vity.freerapid.plugins.services.forshared;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alex, ntoskrnl
 */
class ForSharedRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ForSharedRunner.class.getName());

    private void checkUrl() {
        fileURL = fileURL.replace("/account/", "/").replace("/get/", "/file/");
        addCookie(new Cookie(".4shared.com", "4langcookie", "en", "/", 86400, false));
        addCookie(new Cookie(".4shared.com", "Login", "16802594", "/", 86400, false));
        addCookie(new Cookie(".4shared.com", "Password", "89768e17adf70fa33790fa71abdc8366", "/", 86400, false));
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkUrl();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        checkUrl();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            if (isFolder()) {
                parseFolder();
            } else {
                method = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download").toGetMethod();
                if (makeRedirectedRequest(method)) {
                    checkProblems();
                    method = getMethodBuilder().setReferer(method.getURI().toString()).setActionFromAHrefWhereATagContains("Download file").toGetMethod();
                    downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "id=\"secondsLeft\" value=\"", "\"") + 1);
                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                } else {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Can't load download page");
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException("Can't load download page");
        }
    }

    private void checkNameAndSize() throws Exception {
        if (!isFolder()) {
            PlugUtils.checkName(httpFile, getContentAsString(), "<title>", "- 4shared");
            final Matcher matcher = Pattern.compile("\"fileInfo.+?([\\d,\\.]+ (?:KB|MB|GB))", Pattern.DOTALL).matcher(getContentAsString());
            if (!matcher.find()) {
                throw new PluginImplementationException("File size not found");
            }
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1).replace(",", "")));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }


    private void checkProblems() throws ServiceConnectionProblemException, NotRecoverableDownloadException {
        final String content = getContentAsString();
        if (content.contains("The file link that you requested is not valid")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("already downloading")) {
            throw new ServiceConnectionProblemException("Your IP address is already downloading a file");
        }
        if (content.contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException("Currently a lot of users are downloading files");
        }
        if (content.contains("You must enter a password to access this file")) {
            throw new NotRecoverableDownloadException("Files with password are not supported");
        }
    }

    private boolean isFolder() {
        return fileURL.contains("/dir/") || fileURL.contains("/folder/");
    }

    private void parseFolder() throws Exception {
        final HttpMethod method = getMethodBuilder()
                .setAction("http://www.4shared.com/web/accountActions/changeDir")
                .setParameter("dirId", getFolderId())
                .setAjax()
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        final Matcher matcher = getMatcherAgainstContent("\"id\":\"(.+?)\"");
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find()) {
            final String url = "http://www.4shared.com/file/" + matcher.group(1);
            try {
                uriList.add(new URI(url));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        httpFile.getProperties().put("removeCompleted", true);
    }

    private String getFolderId() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("/(dir|folder)/([^/]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Folder ID not found");
        }
        return matcher.group(1);
    }

}