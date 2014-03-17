package cz.vity.freerapid.plugins.services.ddlanime;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class DdlAnimeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DdlAnimeFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkFileProblems();
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h2>Download File ", "</h2>");
        PlugUtils.checkFileSize(httpFile, content, "</font> (", ")</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();

        logger.info("Starting download in TASK " + fileURL);
        login();

        GetMethod method = getGetMethod(fileURL); //create GET request
        if (!makeRedirectedRequest(method)) { //we make the main request
            logger.warning(getContentAsString());
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }

        HttpMethod httpMethod = getMethodBuilder()
                .setBaseURL(fileURL)
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("Free Download", true)
                .setAction(fileURL)
                .removeParameter("method_premium")
                .toPostMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkDownloadProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }

        //final String contentAsString = getContentAsString();
        checkDownloadProblems();

        //process wait time
        String waitTimeRule = "id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span";
        Matcher waitTimematcher = PlugUtils.matcher(waitTimeRule, getContentAsString());
        if (waitTimematcher.find()) {
            downloadTask.sleep(Integer.parseInt(waitTimematcher.group(1)));
        }

        MethodBuilder methodBuilder = getMethodBuilder()
                .setActionFromFormByName("F1", true)
                .setAction(fileURL)
                .removeParameter("method_premium");

        if (isPassworded()) {
            final String password = getDialogSupport().askForPassword("DDLAnime");
            if (password == null) {
                throw new NotRecoverableDownloadException("This file is secured with a password");
            }
            methodBuilder.setParameter("password", password);
        }

        if (getContentAsString().contains("captcha_code")) { //if contains captcha
            methodBuilder = stepCaptcha(methodBuilder);
        }

        httpMethod = methodBuilder.toPostMethod();

        if (!makeRedirectedRequest(httpMethod)) {
            checkDownloadProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }
        checkDownloadProblems();

        httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromAHrefWhereATagContains(httpFile.getFileName())
                .toGetMethod();

        logger.info("Final URL : " + httpMethod.getURI());

        //here is the download link extraction
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkDownloadProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (PlugUtils.matcher("No such file|File not found|File Not Found", contentAsString).find()) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }

    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("This file reached max downloads limit")) {
            throw new PluginImplementationException("This file reached max downloads limit");
        }
        if (contentAsString.contains("Wrong captcha")) {
            throw new YouHaveToWaitException("Wrong captcha", 1); //let to know user in FRD
        }
    }

    private boolean login() throws Exception {
        synchronized (DdlAnimeFileRunner.class) {
            DdlAnimeServiceImpl service = (DdlAnimeServiceImpl) getPluginService();
            String contentAsString;
            PremiumAccount pa = service.getConfig();

            //for testing purpose
            //pa.setUsername("freerapid");
            //pa.setPassword("freerapid");

            if (pa == null || !pa.isSet()) {
                logger.info("No account data set, skipping login");
                return false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction("http://ddlanime.com/login.html")
                    .setParameter("redirect", "")
                    .setParameter("op", "login")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("submit", "")
                    .toPostMethod();
            addCookie(new Cookie(".ddlanime.com", "login", pa.getUsername(), "/", null, false));
            addCookie(new Cookie(".ddlanime.com", "xfss", "", "/", null, false));

            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");
            contentAsString = getContentAsString();

            if (contentAsString.contains("Incorrect Login or Password"))
                throw new BadLoginException("Invalid DDLAnime registered account login information!");

            return true;
        }
    }

    private boolean isPassworded() {
        boolean passworded = getContentAsString().contains("<input type=\"password\" name=\"password\" class=\"myForm\">");
        return passworded;
    }

    private MethodBuilder stepCaptcha(MethodBuilder methodBuilder) throws Exception {
        //process captcha
        logger.info("Processing captcha");
        final String contentAsString = getContentAsString();
        String captchaRule = "<span style=\\'position:absolute;padding\\-left:(\\d+)px;padding\\-top:\\d+px;\\'>(\\d+)</span>";
        Matcher captchaMatcher = PlugUtils.matcher(captchaRule, PlugUtils.unescapeHtml(contentAsString));
        StringBuffer strbuffCaptcha = new StringBuffer(4);
        SortedMap<Integer, String> captchaMap = new TreeMap<Integer, String>();

        while (captchaMatcher.find()) {
            captchaMap.put(Integer.parseInt(captchaMatcher.group(1)), captchaMatcher.group(2));
        }
        for (String value : captchaMap.values()) {
            strbuffCaptcha.append(value);
        }
        String strCaptcha = Integer.toString(Integer.parseInt(strbuffCaptcha.toString())); //omitting leading '0'
        logger.info("Captcha : " + strCaptcha);

        return methodBuilder.setParameter("code", strCaptcha);
    }

}