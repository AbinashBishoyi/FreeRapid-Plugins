package cz.vity.freerapid.plugins.services.rapidgator_premium;

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
class RapidGatorFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RapidGatorFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".rapidgator.net", "lang", "en", "/", 86400, false));
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (fileURL.contains("/folder/")) {
                httpFile.setFileName("Folder : " + PlugUtils.getStringBetween(getContentAsString(), "<title>Download file", "</title>"));
                httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
            } else
                checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(final String content) throws ErrorDuringDownloadingException {
        final String filenameRegexRule = "Downloading:\\s*</strong>\\s*<a.+?>\\s*(\\S+)\\s*</a>";
        final String filesizeRegexRule = "File size:\\s*<strong>(.+?)</strong>";

        final Matcher filenameMatcher = PlugUtils.matcher(filenameRegexRule, content);
        if (filenameMatcher.find()) {
            httpFile.setFileName(filenameMatcher.group(1));
        } else {
            throw new PluginImplementationException("File name not found");
        }

        final Matcher filesizeMatcher = PlugUtils.matcher(filesizeRegexRule, content);
        if (filesizeMatcher.find()) {
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(filesizeMatcher.group(1)));
        } else {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".rapidgator.net", "lang", "en", "/", 86400, false));
        login();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (fileURL.contains("/folder/")) {
                List<URI> list = new LinkedList<URI>();
                final Matcher m = PlugUtils.matcher("class=\"(?:odd|even)\"><td><a href=\"(.+?)\"", getContentAsString());
                while (m.find()) {
                    list.add(new URI("http://rapidgator.net" + m.group(1).trim()));
                }
                if (list.isEmpty()) throw new PluginImplementationException("No links found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                httpFile.setFileName("Link(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);
                return;
            }
            checkNameAndSize(getContentAsString());
            method = getMethodBuilder().setActionFromTextBetween("var premium_download_link = '", "';").toGetMethod();
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
        if (getContentAsString().contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void login() throws Exception {
        synchronized (RapidGatorFileRunner.class) {
            RapidGatorServiceImpl service = (RapidGatorServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No RapidGator account login information!");
                }
            }
            final HttpMethod method = getMethodBuilder()
                    .setAction("https://rapidgator.net/auth/login")
                    .setParameter("LoginForm[email]", pa.getUsername())
                    .setParameter("LoginForm[password]", pa.getPassword())
                    .setParameter("LoginForm[rememberMe]", "1")
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            if (getContentAsString().contains("Please fix the following input errors")) {
                throw new BadLoginException("Invalid RapidGator account login information!");
            }
        }
    }

}