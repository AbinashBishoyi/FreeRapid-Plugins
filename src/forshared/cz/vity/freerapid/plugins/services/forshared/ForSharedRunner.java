package cz.vity.freerapid.plugins.services.forshared;

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
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Alex, ntoskrnl, tong2shot
 */
class ForSharedRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ForSharedRunner.class.getName());
    private final static Map<Class<?>, LoginData> LOGIN_CACHE = new WeakHashMap<Class<?>, LoginData>(2);


    private void checkUrl() {
        fileURL = fileURL.replace("/account/", "/").replace("/get/", "/file/");
        addCookie(new Cookie(".4shared.com", "4langcookie", "en", "/", 86400, false));
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
        login();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            if (isFolder()) {
                parseFolder();
            } else {
                method = getMethodBuilder().setReferer(fileURL).setActionFromTextBetween("<a id=\"btnLink\" href=\"", "\"").toGetMethod();
                if (!method.getURI().toString().contains("/download/")) {
                    if (makeRedirectedRequest(method)) {
                        checkProblems();
                        method = getMethodBuilder().setReferer(method.getURI().toString()).setActionFromAHrefWhereATagContains("Download file").toGetMethod();
                        downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "id=\"secondsLeft\" value=\"", "\"") + 1);
                    } else {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                }
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        if (!isFolder()) {
            Matcher matcher = getMatcherAgainstContent("\"fileName\\b[^<>]+>(.+?)<");
            if (!matcher.find()) {
                throw new PluginImplementationException("File name not found");
            }
            httpFile.setFileName(matcher.group(1));
            matcher = getMatcherAgainstContent("(?s)\"fileInfo\\b.+?([\\d,\\.]+ .?B) \\|");
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
        return fileURL.contains("/dir/") || fileURL.contains("/folder/") || fileURL.contains("/minifolder/");
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
        final Matcher matcher = PlugUtils.matcher("/(?:dir|folder|minifolder)/([^/]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Folder ID not found");
        }
        return matcher.group(1);
    }

    protected boolean login() throws Exception {
        synchronized (getClass()) {
            final ForSharedServiceImpl service = (ForSharedServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    LOGIN_CACHE.remove(getClass());
                    throw new BadLoginException("No 4Shared account login information!");
                }
            }
            final LoginData loginData = LOGIN_CACHE.get(getClass());
            if (loginData == null || !pa.equals(loginData.getPa()) || loginData.isStale()) {
                logger.info("Logging in");
                doLogin(pa);
                final Cookie[] cookies = new Cookie[2];
                final Cookie loginCookie = getCookieByName("Login");
                final Cookie passwdCookie = getCookieByName("Password");
                if ((loginCookie == null) || (passwdCookie == null)) {
                    throw new PluginImplementationException("Login cookies not found");
                }
                cookies[0] = loginCookie;
                cookies[1] = passwdCookie;
                LOGIN_CACHE.put(getClass(), new LoginData(pa, cookies));
            } else {
                logger.info("Login data cache hit");
                client.getHTTPClient().getState().addCookies(loginData.getCookies());
            }
            return true;
        }
    }


    private void doLogin(final PremiumAccount pa) throws Exception {
        HttpMethod method = getMethodBuilder()
                .setAction("https://www.4shared.com/web/login")
                .setAjax()
                .setParameter("returnTo", fileURL)
                .setParameter("login", pa.getUsername())
                .setParameter("password", pa.getPassword())
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException("Error posting login info");
        }
        if (getContentAsString().contains("Invalid e-mail address or password")) {
            throw new BadLoginException("Invalid 4Shared account login information");
        }

        //language is based on account pref,
        //setting language cookie doesn't work
        Cookie langCookie = getCookieByName("4langcookie");
        if ((langCookie == null) || !langCookie.getValue().equalsIgnoreCase("en")) {
            method = getMethodBuilder()
                    .setAction("http://www.4shared.com/web/user/language") //set account's language preference
                    .setParameter("code", "en")
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            if (!getContentAsString().contains("\"status\":\"ok\"")) {
                throw new PluginImplementationException("Setting language to EN failed");
            }
        }
    }

    private static class LoginData {
        private final static long MAX_AGE = 86400000;//1 day
        private final long created;
        private final PremiumAccount pa;
        private final Cookie[] cookies;

        public LoginData(final PremiumAccount pa, final Cookie[] cookies) {
            this.created = System.currentTimeMillis();
            this.pa = pa;
            this.cookies = cookies;
        }

        public boolean isStale() {
            return System.currentTimeMillis() - created > MAX_AGE;
        }

        public PremiumAccount getPa() {
            return pa;
        }

        public Cookie[] getCookies() {
            return cookies;
        }
    }

}