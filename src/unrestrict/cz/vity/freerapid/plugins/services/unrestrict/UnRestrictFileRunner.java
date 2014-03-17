package cz.vity.freerapid.plugins.services.unrestrict;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.solvemediacaptcha.SolveMediaCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
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
class UnRestrictFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(cz.vity.freerapid.plugins.services.unrestrict.UnRestrictFileRunner.class.getName());
    final String UR_BASE_URL = "http://unrestrict.li";
    final String UR_LOGIN = "http://unrestrict.li/sign_in/";
    final String UR_GENERATE = "http://unrestrict.li/download/";

    @Override
    public void runCheck() throws Exception { //this method validates file
        addCookie(new Cookie(".unrestrict.li", "lang", "EN", "/", 86400, false));
        super.runCheck();
        if (PlugUtils.matcher("unr(estrict)?\\.li/dl/", fileURL).find()) {
            login();
            final GetMethod getMethod = getGetMethod(fileURL);//make first request
            if (makeRedirectedRequest(getMethod)) {
                checkProblems();
                checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher matchN = PlugUtils.matcher("Filename</span></h3></td>\\s*?<td>\\s*?<p>\\s*?(.+?)\\s*?</p></td>", content);
        if (!matchN.find())
            throw new PluginImplementationException("File name not found");
        String fileName = matchN.group(1);
        if (fileName.contains("decode64("))
            fileName = new String(Base64.decodeBase64(PlugUtils.getStringBetween(fileName, "decode64(\"", "\")")));
        httpFile.setFileName(fileName);
        final Matcher matchS = PlugUtils.matcher("File\\s?size</span></h3></td>\\s*?<td><p>(.+?)</p></td>", content);
        if (!matchS.find())
            throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matchS.group(1)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        addCookie(new Cookie(".unrestrict.li", "lang", "EN", "/", 86400, false));
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        if (PlugUtils.matcher("unr(estrict)?\\.li/dl/", fileURL).find()) {
            downloadUnRestrictLink(fileURL);
        } else {
            downloadUnRestrictLink(generateUnRestrictLink());
        }
    }

    public String generateUnRestrictLink() throws Exception {
        final GetMethod method = getGetMethod(UR_GENERATE);
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            final HttpMethod postMethod = getMethodBuilder()
                    .setParameter("link", fileURL)
                    .setParameter("domain", "long")
                    .setAction(UR_BASE_URL + "/unrestrict.php")
                    .setReferer(UR_GENERATE)
                    .toPostMethod();
            postMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRequest(postMethod)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            checkRequest();

            return PlugUtils.getStringBetween(getContentAsString(), "{\"", "\":").replace("\\/", "/");
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    public void downloadUnRestrictLink(final String dlLink) throws Exception {
        final GetMethod getMethod = getGetMethod(dlLink);//make first request
        final int status = client.makeRequest(getMethod, false);
        if (status / 100 == 3) {    // VIP direct download
            if (!tryDownloadAndSaveFile(getGetMethod(getMethod.getResponseHeader("Location").getValue()))) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else if (status == 200) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
            boolean captchaLoop;
            do {
                captchaLoop = false;
                final HttpMethod dlMethod = doCaptcha(getMethodBuilder()
                        .setAction(UR_BASE_URL + "/download.php")
                        .setParameter("link", PlugUtils.getStringBetween(getContentAsString(), "link\" type=\"hidden\" value=\"", "\" />"))
                        .setReferer(dlLink)
                        , false).toPostMethod();
                dlMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
                if (!makeRedirectedRequest(dlMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                checkRequest();
                if (getContentAsString().contains("error\":\"Incorrect captcha entered")) {
                    captchaLoop = true;
                    if (!makeRedirectedRequest(getMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();
                }
            } while (captchaLoop);
            final String dlUrl = PlugUtils.getStringBetween(getContentAsString(), "link\":\"", "\"}").replace("\\/", "/");
            final HttpMethod download = getGetMethod(dlUrl);
            download.removeRequestHeader("Referer");
            if (!tryDownloadAndSaveFile(download)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkRequest() throws Exception {
        final String content = getContentAsString();
        if (content.contains("error\":\"Failed to get file link"))
            throw new ServiceConnectionProblemException("Failed to get download url. Link expired ?");
        if (content.contains("invalid\":\"Host has been disabled"))
            throw new ErrorDuringDownloadingException("Download from this host not possible at the moment");
        if (content.contains("invalid\":\"You are not allowed to download from this host"))
            throw new NotRecoverableDownloadException("You need to upgrade membership to download from this host");
        if (content.contains("invalid\":\"Host is not supported or unknown link format"))
            throw new NotRecoverableDownloadException("Host is not supported or unknown link format");
        if (content.contains("invalid\":\"Invalid\\/Offline Video."))
            throw new URLNotAvailableAnymoreException("Link is not available");
    }

    private void checkProblems() throws Exception {
        final String content = getContentAsString();
        if (content.contains("class=\"invalid\">Invalid link"))
            throw new NotRecoverableDownloadException("Invalid link, expired or membership upgrade required");
        if (content.contains("An error occured while processing your download"))
            throw new ErrorDuringDownloadingException("An error occured while processing your download");
    }


    private static String loginCookie = "";
    private static PremiumAccount pa0 = null;

    private void login() throws Exception {
        synchronized (cz.vity.freerapid.plugins.services.unrestrict.UnRestrictFileRunner.class) {
            cz.vity.freerapid.plugins.services.unrestrict.UnRestrictServiceImpl service = (cz.vity.freerapid.plugins.services.unrestrict.UnRestrictServiceImpl) getPluginService();
            final PremiumAccount pa = service.getConfig();
            if (pa.isSet()) {
                if (pa != pa0) {
                    pa0 = pa;
                    loginCookie = "";
                }
                if (!loginCookie.equals("")) {
                    logger.warning("Logging in using cookie");
                    addCookie(new Cookie(".unrestrict.li", "unrestrict_user", loginCookie, "/", 86400, false));
                    addCookie(new Cookie(".unrestrict.li", "ssl", "0", "/", 86400, false));
                } else {
                    logger.warning("Logging in using form");
                    HttpMethod method = getGetMethod(UR_LOGIN);
                    do {
                        if (!makeRedirectedRequest(method)) {
                            checkProblems();
                            throw new ServiceConnectionProblemException();
                        }
                        checkProblems();//check problems
                        method = doCaptcha(getMethodBuilder()
                                .setAction(UR_LOGIN)
                                .setParameter("username", pa.getUsername())
                                .setParameter("password", pa.getPassword())
                                .setParameter("signin", "Sign in")
                                .setBaseURL(UR_BASE_URL)
                                .setReferer(UR_LOGIN)
                                , true).toPostMethod();
                        if (!makeRedirectedRequest(method)) {
                            throw new ServiceConnectionProblemException("Error posting login info");
                        }
                        if (getContentAsString().contains("invalid\">Incorrect username or password")) {
                            throw new BadLoginException("Incorrect username or password");
                        }
                    } while (getContentAsString().contains("invalid\">Incorrect captcha entered"));
                    if (!getContentAsString().contains("http://unrestrict.li/sign_out"))
                        throw new BadLoginException("Error logging in");
                    loginCookie = getCookieByName("unrestrict_user").getValue();
                }
            }
        }
    }

    private MethodBuilder doCaptcha(MethodBuilder builder, final boolean stdParams) throws Exception {
        if (getContentAsString().contains("solvemedia.com")) {
            final Matcher m = getMatcherAgainstContent("challenge\\.(?:no)?script\\?k=(.+?)\"");
            if (!m.find()) throw new PluginImplementationException("Captcha key not found");
            final String captchaKey = m.group(1);
            final SolveMediaCaptcha solveMediaCaptcha = new SolveMediaCaptcha(captchaKey, client, getCaptchaSupport(), true);
            solveMediaCaptcha.askForCaptcha();
            if (stdParams) {
                solveMediaCaptcha.modifyResponseMethod(builder);
            } else {
                builder.setParameter("challenge", solveMediaCaptcha.getChallenge());
                builder.setParameter("response", solveMediaCaptcha.getResponse());
            }
        }
        return builder;
    }

}