package cz.vity.freerapid.plugins.services.ifile_login;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.ifile.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * @author JPEXS
 * @since 0.83
 */
class IFileFileRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(IFileFileRunner.class.getName());
    private final static String BASE_URL = "http://ifile.it/";
    private final static String REDIRECT_URL = BASE_URL + "dl";
    private String __x_fsa;
    private String __x_fs;
    private String __x_c;
    private String __esn;
    private String __recaptcha_public;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).setEncodePathAndQuery(true).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkSeriousProblems();
            checkNameAndSize();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void finalRequest() throws Exception {
        final HttpMethod method = getMethodBuilder().setAction(REDIRECT_URL).setReferer(REDIRECT_URL).toHttpMethod();
        makeRedirectedRequest(method);
        final HttpMethod finalMethod = getMethodBuilder().setActionFromAHrefWhereATagContains("download").setReferer(REDIRECT_URL).toHttpMethod();
        if (!tryDownloadAndSaveFile(finalMethod)) {
            logger.warning(getContentAsString());
            throw new IOException("File input stream is empty.");
        }
    }

    private String forcedGetContentAsString(HttpMethod method) {

        String content = null;
        try {
            content = method.getResponseBodyAsString();
        } catch (IOException ex) {
            //ignore
        }
        if (content == null) {
            content = "";
            InputStream is = null;
            try {
                is = method.getResponseBodyAsStream();
                if (is != null) {
                    int i = 0;
                    while ((i = is.read()) != -1) {
                        content += (char) i;
                    }
                }
            } catch (IOException ex) {
                //ignore
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        //ignore
                    }
                }
            }

        }
        return content;
    }

    private void makeUrl(String a, String b) throws Exception {
        String c = BASE_URL + "download:dl_request?" + __x_fsa + "&type=" + a + "&esn=" + __esn + b;
        c += "&" + __x_fs;
        HttpMethod method = getMethodBuilder().setAction(c).toHttpMethod();
        method.addRequestHeader("X-Requested-With", "XMLHttpRequest"); //We use AJAX :-)

        /**
         * Note:
         * Because server response content type is json,
         * internal function getContentAsString does not work.
         */
        if (client.getHTTPClient().executeMethod(method) == 200) {
            String content = forcedGetContentAsString(method);
            //Response looks like this: {"status":"ok","captcha":"none","retry":"none"}
            String respStatus = PlugUtils.getStringBetween(content, "\"status\":\"", "\"");
            String respCaptcha = PlugUtils.getStringBetween(content, "\"captcha\":\"", "\"");
            // Retry is not used...
            //String respRetry = PlugUtils.getStringBetween(content, "\"retry\":\"", "\"");
            if (respStatus.equals("ok")) {
                MethodBuilder mb = getMethodBuilder().setAction(REDIRECT_URL).setReferer(REDIRECT_URL);
                if (respCaptcha.equals("none")) {
                    finalRequest();
                } else if (respCaptcha.equals("recaptcha")) {
                    stepReCaptcha();
                } else {
                    stepCaptchaSimple();
                }
            } else {
                throw new PluginImplementationException("Server returned wrong status: " + respStatus);
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).setEncodePathAndQuery(true).toHttpMethod();
        checkLogin();
        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkNameAndSize();
            String content = getContentAsString();            
            __x_fsa = PlugUtils.getStringBetween(content, "var __x_fsa = '", "';");
            __x_fs = PlugUtils.getStringBetween(content, "var __x_fs = '", "';");
            __x_c = PlugUtils.getStringBetween(content, "var __x_c = '", "';");
//            __esn = PlugUtils.getStringBetween(getContentAsString(), "var	__esn = ", ";");
            __esn = "0";
            __recaptcha_public = PlugUtils.getStringBetween(content, "var __recaptcha_public		=	'", "';");
            makeUrl("na", "");
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file removed") || contentAsString.contains("no such file") || contentAsString.contains("file expired")) {
            throw new URLNotAvailableAnymoreException("File was not found");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        checkLoginProblems();
    }

    private void checkLoginProblems() throws BadLoginException{
        if(getContentAsString().contains("sign in")){
            badConfig=true;
            throw new BadLoginException("Bad login or password");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("gray;\">\\s*([^<]*)\\s*&nbsp;\\s*([^<]*)\\s*</span>");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);
            final long fileSize = PlugUtils.getFileSizeFromString(matcher.group(2));
            logger.info("File size " + fileSize);
            httpFile.setFileSize(fileSize);
        } else {
            logger.warning("File size and size were not found");
            throw new PluginImplementationException("File name and size not found");
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void stepReCaptcha() throws Exception {
        ReCaptcha r = new ReCaptcha(__recaptcha_public, client);
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = r.getImageURL();
        logger.info("ReCaptcha URL " + captchaSrc);
        final String captcha = captchaSupport.getCaptcha(captchaSrc);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            r.setRecognized(captcha);
            makeUrl("recaptcha", "&" + r.getResponseParams());
        }
    }

    private void stepCaptchaSimple() throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = BASE_URL + "download:captcha";
        logger.info("Simple captcha URL " + captchaSrc);
        final String captcha = captchaSupport.getCaptcha(captchaSrc);

        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            makeUrl("simple", "&" + __x_c + "=" + captcha);
        }
    }

    private void checkLogin() throws Exception {
        synchronized (IFileFileRunner.class) {
            IFileServiceImpl service = (IFileServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (badConfig) {
                pa = service.showConfigDialog();
                badConfig = false;
            }
            if (pa == null || !pa.isSet()) {
                return; //nologin
            }

            cookie = login(pa.getUsername(), pa.getPassword());
            logger.info("Builded IFile cookie: " + cookie);

            client.getHTTPClient().getState().addCookie(new Cookie(".ifile.it", "ifileit_auth", cookie, "/", 86400, false));
        }
    }

    private String login(String login, String password) throws IOException, BadLoginException {
        if (IFileFileRunner.cookie != null) {
            return IFileFileRunner.cookie;
        }

        final PostMethod pm = getPostMethod("https://secure.ifile.it/account:process_signin?redirect_after=0");
        pm.addParameter("usernameFld", login);
        pm.addParameter("passwordFld", password);
        pm.addParameter("submitBtn", "continue");
        int res=client.makeRequest(pm, false);
        pm.releaseConnection();
        if(res==301){
            badConfig=true;
            throw new BadLoginException("Bad login or password");
        }
        ////ifileit_auth=e942dfc474f1ff703cea17d26119f1891585740; expires=Sun, 08-Nov-2009 12:03:27 GMT; path=/; domain=.ifile.it

        Cookie[] cookies = client.getHTTPClient().getState().getCookies();
        for (Cookie c : cookies) {
            if ("ifileit_auth".equals(c.getName())) {
                IFileFileRunner.cookie = c.getValue();
                return c.getValue();
            }
        }
        return null;
    }
    private boolean badConfig = false;
    private static String cookie = null;
}
