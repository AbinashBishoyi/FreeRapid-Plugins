package cz.vity.freerapid.plugins.services.hellspy;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class HellSpyFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(HellSpyFileRunner.class.getName());
    private final static String SERVICE_WEB = "http://www.hellspy.com/";
    private static List<Cookie> cookies;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            setLanguage();
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
        checkURL();
        logger.info("Starting download in TASK " + fileURL);

        login();

        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            if (getContentAsString().contains("You must be signed-in to download files")) {
                throw new BadLoginException("Failed to log in");
            }
            String nextURL=PlugUtils.getStringBetween(getContentAsString(), "href=\"", "\" target=\"downloadIframe\"");
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(SERVICE_WEB).setAction(nextURL).toGetMethod();
            if(nextURL.endsWith("?freedownload=1")){ //Sometimes you can download for free.
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting Free download");
                }
                return;
            }
            if (makeRedirectedRequest(httpMethod)) {
                if (!getContentAsString().contains("<a ")) {
                    throw new NotRecoverableDownloadException("Either your account does not have enough credit, or the plugin is broken");
                }

                httpMethod = getMethodBuilder().setReferer(httpMethod.getURI().toString()).setActionFromTextBetween("launchFullDownload('", "',").toGetMethod();//setActionFromAHrefWhereATagContains("zde").toGetMethod();
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkURL() {
        if (fileURL.contains("?")) {
            fileURL = fileURL.substring(0, fileURL.indexOf('?'));
        }
        fileURL = fileURL.replaceFirst("(?i)http://([a-z]+?\\.)?hellspy\\.([a-z]{2,3})/", "http://www.hellspy.com/");
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "<span class=\"text\" title=\"\">", "</span></span></h1>");
        final String size = PlugUtils.getStringBetween(content, "Size: </span>", "</span>").replace("&nbsp;", " ");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Page not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void setLanguage() throws Exception {
        String phpsess="";
        for (final Cookie cookie : client.getHTTPClient().getState().getCookies()) {
                    if(cookie.getName().equals("PHPSESSID")){
                        phpsess=cookie.getValue();
                    }
        }
        Matcher m=Pattern.compile("http://([a-z]+?\\.)?hellspy\\.([a-z]{2,3})/(.+)").matcher(fileURL);
        if(m.find()){
            final HttpMethod httpMethod = getMethodBuilder()
                .setReferer(SERVICE_WEB)
                .setAction("http://"+m.group(1)+"hellspy.com/--"+phpsess+"-/"+m.group(3))
                //.setActionFromAHrefWhereATagContains("English")
                .toGetMethod();
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException();
        }
    }

    private void login() throws Exception {
        synchronized (HellSpyFileRunner.class) {
            if (cookies == null) {
                logger.info("Logging in");
                HttpMethod httpMethod = getGetMethod(SERVICE_WEB);
                if (!makeRedirectedRequest(httpMethod))
                    throw new ServiceConnectionProblemException();
                setLanguage();
                HellSpyServiceImpl service = (HellSpyServiceImpl) getPluginService();
                PremiumAccount pa = service.getConfig();
                if (!pa.isSet()) {
                    pa = service.showConfigDialog();
                    if (pa == null || !pa.isSet()) {
                        throw new BadLoginException("No HellSpy login information!");
                    }
                }

                /*final Matcher lg = getMatcherAgainstContent("name=\"(.+?_lg)\"");
                final Matcher psw = getMatcherAgainstContent("name=\"(.+?_psw)\"");
                if (!lg.find() || !psw.find())
                    throw new PluginImplementationException("Login form not found");
                */
                httpMethod = getMethodBuilder()
                        .setReferer(SERVICE_WEB)
                        .setActionFromFormWhereTagContains("Login", true)
                        .setParameter("username", pa.getUsername())
                        .setParameter("password",pa.getPassword())
                        .toPostMethod();
                if (!makeRedirectedRequest(httpMethod))
                    throw new ServiceConnectionProblemException("Error posting login info");

                if (getContentAsString().contains("Wrong user or password"))
                    throw new BadLoginException("Invalid HellSpy login information!");
                cookies=new ArrayList<Cookie>();
                //There are 3 PHPSESSID cookies, save each of them...
                for (final Cookie cookie : client.getHTTPClient().getState().getCookies()) {
                    cookies.add(cookie);
                }
            } else {
                for(Cookie c:cookies)
                    addCookie(c);
                HttpMethod httpMethod = getGetMethod(SERVICE_WEB);
                if (!makeRedirectedRequest(httpMethod))
                    throw new ServiceConnectionProblemException();
                setLanguage();
            }

            HttpMethod httpMethod = getGetMethod(SERVICE_WEB);
                if (!makeRedirectedRequest(httpMethod))
                    throw new ServiceConnectionProblemException();
                setLanguage();

        }
    }


}