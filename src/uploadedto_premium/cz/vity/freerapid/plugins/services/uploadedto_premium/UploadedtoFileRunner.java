package cz.vity.freerapid.plugins.services.uploadedto_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

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
        final Matcher matcher = getMatcherAgainstContent("<title>(.+?) \\((.+?)\\) \\- uploaded\\.(?:to|net)</title>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name/size not found");
        }
        httpFile.setFileName(PlugUtils.unescapeHtml(matcher.group(1)));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".uploaded.net", "lang", "en", "/", 86400, false));
        login();
        HttpMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            checkNameAndSize();
            method = getMethodBuilder().setActionFromFormWhereTagContains("Premium Download", true).toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
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