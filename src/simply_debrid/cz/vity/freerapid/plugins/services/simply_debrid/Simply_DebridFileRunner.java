package cz.vity.freerapid.plugins.services.simply_debrid;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class Simply_DebridFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Simply_DebridFileRunner.class.getName());
    final String SD_BASE_URL = "http://simply-debrid.com/";
    final String SD_LOGIN = "http://simply-debrid.com/login";
    final String SD_GENERATE = "http://simply-debrid.com/generate";

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        final GetMethod method = getGetMethod(SD_GENERATE);
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            final HttpMethod postMethod = getMethodBuilder()
                    .setActionFromFormWhereActionContains("generate", false)
                    .setParameter("link", fileURL)
                    .setParameter("submit2", "Generate your links")
                    .setAction(SD_GENERATE)
                    .setReferer(SD_GENERATE)
                    .toPostMethod();
            if (!makeRequest(postMethod)) {
                throw new ServiceConnectionProblemException("Error posting link request");
            }
            final HttpMethod httpMethod = getGetMethod(SD_BASE_URL + PlugUtils.getStringBetween(getContentAsString(), "$.get(\"", "\","));
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final HttpMethod dlMethod = getGetMethod(PlugUtils.getStringBetween(getContentAsString(), "<a href=\"", "\">"));
            httpFile.setFileName(PlugUtils.getStringBetween(getContentAsString(), "\">", "</a>"));
            dlMethod.removeRequestHeader("Accept-Encoding");
            dlMethod.addRequestHeader("Host", PlugUtils.getStringBetween(getContentAsString(), "=\"http://", "/"));
            setClientParameter(DownloadClientConsts.NO_CONTENT_TYPE_IN_HEADER, true);
            if (!tryDownloadAndSaveFile(dlMethod)) {
                checkProblems();//if downloading failed
                created = 0;
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Invalid link")) {
            throw new URLNotAvailableAnymoreException("Invalid link");
        }
        if (content.contains("This particular host is not available for non PREMIUM Members")) {
            throw new NotRecoverableDownloadException("This host is for Premium users only");
        }
        if (content.contains("Your free daily limit has been reached")) {
            throw new ErrorDuringDownloadingException("Your free daily limit has been reached");
        }
    }

    private final static long MAX_AGE = 12 * 3600000;//12 hours
    private static long created = 0;
    private static Cookie sessionId;
    private static PremiumAccount pa0 = null;

    public boolean isLoginStale(final PremiumAccount pa) {
        return (System.currentTimeMillis() - created > MAX_AGE) || (!pa0.getUsername().matches(pa.getUsername())) || (!pa0.getPassword().matches(pa.getPassword()));
    }

    private void login() throws Exception {
        synchronized (Simply_DebridFileRunner.class) {
            Simply_DebridServiceImpl service = (Simply_DebridServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (pa.isSet()) {
                if (!isLoginStale(pa)) {
                    addCookie(sessionId);
                    if (!makeRedirectedRequest(getGetMethod(SD_GENERATE))) {
                        throw new ServiceConnectionProblemException("Error posting link request");
                    }
                    if (getContentAsString().contains("href=\"login\">Login")) {
                        created = 0;
                        throw new YouHaveToWaitException("Login session timed out ... Retrying", 3);
                    }
                    logger.info("Logged in with cookie");
                } else {
                    HttpMethod method = getGetMethod(SD_LOGIN);
                    if (!makeRedirectedRequest(method)) { //we make the main request
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();//check problems
                    int loopCount = 0;
                    do {
                        loopCount++;
                        if (loopCount > 5)
                            throw new BadLoginException("Many Incorrect captcha responses OR Invalid account login information!");
                        method = doCaptcha(getMethodBuilder()
                                .setActionFromFormWhereActionContains("login", true)
                                .setParameter("username", pa.getUsername())
                                .setParameter("password", pa.getPassword())
                                .setParameter("submitButton", "")
                                .setBaseURL(SD_BASE_URL)
                                .setReferer(SD_LOGIN)
                        ).toPostMethod();
                        if (!makeRedirectedRequest(method)) {
                            throw new ServiceConnectionProblemException("Error posting login info");
                        }
                    } while (!getContentAsString().contains("document.location.href=\"generate\";"));
                    if (getContentAsString().contains("Username or password invalid")) {
                        throw new BadLoginException("Invalid Simply-Debrid account login information!");
                    }
                    pa0 = pa;
                    sessionId = getCookieByName("PHPSESSID");
                    created = System.currentTimeMillis();
                }
            }
        }
    }

    private MethodBuilder doCaptcha(final MethodBuilder builder) throws Exception {
        if (getContentAsString().contains("showRecaptcha('")) {
            String key = PlugUtils.getStringBetween(getContentAsString(), "Recaptcha.create(\"", "\"");
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