package cz.vity.freerapid.plugins.services.filesonic_premium;

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
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author valankar
 * @author ntoskrnl
 */
class FileSonicFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileSonicFileRunner.class.getName());

    private void ensureENLanguage() throws Exception {
        final String domain = new URI(getMethodBuilder().getBaseURL()).getHost();
        addCookie(new Cookie(domain, "lang", "en", "/", 86400, false));
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        ensureENLanguage();
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
        if (!isFolder()) {
            final String content = getContentAsString();
            PlugUtils.checkName(httpFile, content, "<title>Download", "for free on Filesonic.com</title>");
            final String size = PlugUtils.getStringBetween(content, "<span class=\"fileSize\">", "</span>");
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(size.replace(",", "")));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean isFolder() {
        return fileURL.contains("/folder/");
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();
        if (isFolder()) {
            handleFolder();
            return;
        }
        login();
        final HttpMethod method = getGetMethod(fileURL);
        setFileStreamContentTypes("\"application/octet-stream\"");
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    public void login() throws Exception {
        FileSonicServiceImpl service = (FileSonicServiceImpl) getPluginService();
        PremiumAccount pa = service.getConfig();
        synchronized (FileSonicFileRunner.class) {
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No FileSonic Premium account login information!");
                }
            }
        }
        final HttpMethod method = getMethodBuilder()
                .setAction("/user/login")
                .setParameter("email", pa.getUsername())
                .setParameter("redirect", "/")
                .setParameter("password", pa.getPassword())
                .setParameter("rememberMe", "1")
                .toPostMethod();
        method.setRequestHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        method.setRequestHeader("X-Requested-With", "XMLHttpRequest");
        setFileStreamContentTypes(new String[0], new String[]{"application/json"});
        if (!makeRedirectedRequest(method)) {
            logger.warning("Request URL: " + method.getURI());
            logger.warning("Response status line: " + method.getStatusLine());
            logger.warning("Response content: " + getContentAsString());
            throw new ServiceConnectionProblemException("Error posting login info");
        }
        if (!getContentAsString().contains("\"status\":\"success\"")) {
            logger.warning("Request URL: " + method.getURI());
            logger.warning("Response status line: " + method.getStatusLine());
            logger.warning("Response content: " + getContentAsString());
            throw new NotRecoverableDownloadException("Invalid FileSonic Premium account login information!");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File not found") || content.contains("This file was deleted") || content.contains("The requested folder do not exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void handleFolder() throws ErrorDuringDownloadingException {
        final List<URI> list = new LinkedList<URI>();
        final Matcher matcher = getMatcherAgainstContent("<a href=\"(http://(?:www\\.)?filesonic\\.[a-z]{2,3}(?:\\.[a-z]{2,3})?/file/.+?)\">");
        while (matcher.find()) {
            try {
                list.add(new URI(matcher.group(1)));
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.getProperties().put("removeCompleted", true);
    }

}