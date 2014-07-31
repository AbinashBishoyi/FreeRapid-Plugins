package cz.vity.freerapid.plugins.services.keep2share_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class Keep2Share_PremiumFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Keep2Share_PremiumFileRunner.class.getName());
    private final static String loginURL = "http://k2s.cc/login.html";
    private final static String baseURL = "http://k2s.cc/";

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        addCookie(new Cookie(".k2s.cc", "lang", "en", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("<img width.+?>\\s*?(.+?)\\s*?</span>", content);
        if (match.find()) {
            httpFile.setFileName(match.group(1).trim());
            PlugUtils.checkFileSize(httpFile, content, "File size", "</div>");
        } else {
            PlugUtils.checkName(httpFile, content, "File: <span>", "<");
            PlugUtils.checkFileSize(httpFile, content, "Size:", "<");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void redirectURL() {
        fileURL = fileURL.replaceFirst("((keep2share|k2share|keep2s|k2s)\\.cc|keep2share\\.com)", "k2s.cc");
    }

    @Override
    public void run() throws Exception {
        super.run();
        redirectURL();
        addCookie(new Cookie(".k2s.cc", "lang", "en", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        login();
        final HttpMethod method = getMethodBuilder().setBaseURL(baseURL).setAction(fileURL).toGetMethod();
        final int status = client.makeRequest(method, false);
        if (status / 100 == 3) {
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else if (status == 200) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            if (!contentAsString.contains("This link will be available for")) {
                if (getContentAsString().contains("window.location.href")) {
                    HttpMethod hMethod = getMethodBuilder()
                            .setReferer(fileURL).setBaseURL(baseURL)
                            .setAction(PlugUtils.getStringBetween(getContentAsString(), "window.location.href = '", "';"))
                            .toGetMethod();
                    if (!tryDownloadAndSaveFile(hMethod)) {
                        checkProblems();//if downloading failed
                        throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                    }
                    return;
                }
                if (getContentAsString().contains("href=\"/login.html\">Login")) {
                    created = 0;
                    throw new YouHaveToWaitException("Login Error ... Retrying", 3);
                }
                throw new PluginImplementationException("Unknown error - site may have changed");
            }
            final HttpMethod httpMethod = getMethodBuilder().setBaseURL(baseURL).setActionFromAHrefWhereATagContains("this link").toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File not found") || content.contains("<title>Error 404</title>")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (content.contains("Traffic limit exceed")) {
            throw new ServiceConnectionProblemException("Traffic limit exceed");
        }
        final Matcher waitMatch = PlugUtils.matcher("Please wait (\\d+?):(\\d+?):(\\d+?) to download this file", content);
        if (waitMatch.find()) {
            final int waitTime = Integer.parseInt(waitMatch.group(3)) + 60 * (Integer.parseInt(waitMatch.group(2)) + 60 * Integer.parseInt(waitMatch.group(1)));
            throw new YouHaveToWaitException("Please wait for download", waitTime);
        }
    }

    private final static long MAX_AGE = 6 * 3600000;//6 hours
    private static long created = 0;
    private static Cookie sessionId;
    private static PremiumAccount pa0 = null;

    public void setLoginData(final PremiumAccount pa) {
        pa0 = pa;
        sessionId = getCookieByName("sessid");
        created = System.currentTimeMillis();
    }

    public boolean isLoginStale(final PremiumAccount pa) {
        return (System.currentTimeMillis() - created > MAX_AGE) || (!pa0.getUsername().matches(pa.getUsername())) || (!pa0.getPassword().matches(pa.getPassword()));
    }

    private void login() throws Exception {
        synchronized (Keep2Share_PremiumFileRunner.class) {
            Keep2Share_PremiumServiceImpl service = (Keep2Share_PremiumServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No Keep2Share Premium account login information!");
                }
            }
            if (!isLoginStale(pa)) {
                addCookie(sessionId);
            } else {
                final GetMethod getMethod = getGetMethod(loginURL);//make first request
                if (!makeRedirectedRequest(getMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                do {
                    final HttpMethod httpMethod = doCaptcha(getMethodBuilder()
                            .setActionFromFormWhereTagContains("LoginForm", true)
                            .setParameter("LoginForm[username]", pa.getUsername())
                            .setParameter("LoginForm[password]", pa.getPassword())
                            .setParameter("LoginForm[rememberMe]", "1")
                            .setReferer(loginURL).setBaseURL(baseURL)
                    ).toPostMethod();
                    final int status = client.makeRequest(httpMethod, false);
                    if (status / 100 == 3) {
                        // successfully logged in (trying to redirect to dl url)
                        setLoginData(pa);
                        return;
                    } else if (status / 100 != 2) {
                        throw new ServiceConnectionProblemException("Error posting login info");
                    }
                } while (getContentAsString().contains("The verification code is incorrect"));
                if (getContentAsString().contains("Incorrect username or password"))
                    throw new BadLoginException("Incorrect username or password!");
                if (getContentAsString().contains("Your account has been banned"))
                    throw new BadLoginException("Your account has been banned!");
                if (getContentAsString().contains("Please fix the following input errors"))
                    throw new BadLoginException("Login error occurred!");
                // login successful
                setLoginData(pa);
            }
        }
    }

    private MethodBuilder doCaptcha(final MethodBuilder builder) throws Exception {
        if (getContentAsString().contains("recaptcha/api/noscript?k=")) {
            String key = PlugUtils.getStringBetween(getContentAsString(), "recaptcha/api/noscript?k=", "\"");
            final ReCaptcha reCaptcha = new ReCaptcha(key, client);
            final String captchaTxt = getCaptchaSupport().getCaptcha(reCaptcha.getImageURL());
            if (captchaTxt == null)
                throw new CaptchaEntryInputMismatchException();
            reCaptcha.setRecognized(captchaTxt);
            return reCaptcha.modifyResponseMethod(builder);
        }
        return builder;
    }
}