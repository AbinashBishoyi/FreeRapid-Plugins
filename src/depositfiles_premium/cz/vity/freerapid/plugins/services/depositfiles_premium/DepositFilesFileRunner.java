package cz.vity.freerapid.plugins.services.depositfiles_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class DepositFilesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DepositFilesFileRunner.class.getName());
    private boolean badConfig = false;

    private void setLanguageEN() {
        addCookie(new Cookie(".depositfiles.com", "lang_current", "en", "/", 86400, false));
        addCookie(new Cookie(".depositfiles.org", "lang_current", "en", "/", 86400, false));
        addCookie(new Cookie(".dfiles.eu", "lang_current", "en", "/", 86400, false));
        fileURL = fileURL.replaceFirst("/[^/]{2}/(files|folders)/", "/$1/"); // remove language id from URL
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setLanguageEN();
        final GetMethod method = getGetMethod(fileURL);
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
        setLanguageEN();
        logger.info("Starting download in TASK " + fileURL);

        if (isFolder()) {
            runFolder();
            httpFile.getProperties().put("removeCompleted", true);
            return;
        }

        login();

        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            if (getContentAsString().contains("Advantages of the Gold account"))
                throw new BadLoginException("Problem logging in, account not premium?");

            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("Download the file")
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("eval\\s*\\(\\s*unescape\\s*\\(\\s*'(.+?)'");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found (1)");
        }
        final String nameContent = PlugUtils.unescapeUnicode(matcher.group(1).replace("%u", "\\u"));
        matcher = PlugUtils.matcher("File name: <b title=\"(.+?)\"", nameContent);
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found (2)");
        }
        httpFile.setFileName(matcher.group(1));

        matcher = getMatcherAgainstContent("File size: <b[^<>]*?>(.+?)</b>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("file does not exist") || content.contains("<h1>404 Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException(String.format("File not found"));
        }
        if (content.contains("File is checked")) {
            throw new YouHaveToWaitException("File is checked, please try again in a minute", 60);
        }
    }

    private void login() throws Exception {
        synchronized (DepositFilesFileRunner.class) {
            DepositFilesServiceImpl service = (DepositFilesServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet() || badConfig) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No DepositFiles Premium account login information!");
                }
                badConfig = false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction("/login.php")
                    .setParameter("go", "1")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");

            if (getContentAsString().contains("Your password or login is incorrect"))
                throw new BadLoginException("Invalid DepositFiles Premium account login information!");
        }
    }

    private boolean isFolder() {
        return fileURL.contains("/folders/");
    }

    public void runFolder() throws Exception {
        final List<URI> uriList = new LinkedList<URI>();

        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();

            int i = 1;
            final Matcher pages = getMatcherAgainstContent("href=\".+?\\?page=(\\d+?)\">\\d");
            do {
                final MethodBuilder mb = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(fileURL);
                if (i > 1) mb.setParameter("page", String.valueOf(i));
                mb.setParameter("format", "text");
                if (!makeRedirectedRequest(mb.toGetMethod())) throw new ServiceConnectionProblemException();

                final BufferedReader reader = new BufferedReader(new StringReader(getContentAsString().replaceAll("(<.+?>)", "")));
                String s;
                while ((s = reader.readLine()) != null) {
                    if (!s.isEmpty()) {
                        try {
                            uriList.add(new URI(s));
                        } catch (URISyntaxException e) {
                            LogUtils.processException(logger, e);
                        }
                    }
                }
                i++;
            } while (pages.find());

            if (uriList.isEmpty()) throw new URLNotAvailableAnymoreException("No links found");
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

}