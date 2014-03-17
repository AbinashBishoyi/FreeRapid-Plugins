package cz.vity.freerapid.plugins.services.maknyos;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author zid
 */
class MaknyosRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MaknyosRunner.class.getName());

    private boolean login() throws Exception {
        synchronized (MaknyosRunner.class) {
            final MaknyosServiceImpl service = (MaknyosServiceImpl) getPluginService();
            final String contentAsString = getContentAsString();
            PremiumAccount pa = service.getConfig();

            if (pa == null || !pa.isSet()) {
                logger.info("No account data set, skipping login");
                return false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction("http://www.maknyos.com/login.html")
                    .setParameter("redirect", "")
                    .setParameter("op", "login")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("submit", "")
                    .toPostMethod();
            addCookie(new Cookie(".maknyos.com", "login", pa.getUsername(), "/", null, false));
            addCookie(new Cookie(".maknyos.com", "xfss", "", "/", null, false));

            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");
            if (contentAsString.contains("Login dan Kata sandi tidak tepat"))
                throw new BadLoginException("Invalid account login information!");
            if (contentAsString.contains("Incorrect Login or Password"))
                throw new BadLoginException("Invalid Maknyos registered account login information!");

            return true;
        }
    }


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
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
        final String nameAndSizeRule = "(?:Anda telah meminta|You have requested).*?http://(?:www\\.)?" + "maknyos.com" + "/[a-z0-9]{12}/(.*)</font> \\((.*?)\\)</font>$";
        Matcher matcher = PlugUtils.matcher(nameAndSizeRule, content);
        if (matcher.find()) {
            httpFile.setFileName(matcher.group(1));
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));

            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else throw new ErrorDuringDownloadingException();
    }


    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        login();

        final GetMethod method1 = getGetMethod(fileURL);
        if (makeRedirectedRequest(method1)) {
            String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);

            final HttpMethod method2 = getMethodBuilder()
                    .setAction(fileURL)
                    .setBaseURL(fileURL)
                    .setReferer(fileURL)
                    .setActionFromFormByIndex(2, true)
                    .removeParameter("method_premium")
                    .toHttpMethod();
            if (makeRedirectedRequest(method2)) {
                contentAsString = getContentAsString();
                checkSecondaryProblems();

                //process wait time
                String waitTimeRule = "id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span";
                Matcher waitTimematcher = PlugUtils.matcher(waitTimeRule, contentAsString);
                if (waitTimematcher.find()) {
                    downloadTask.sleep(Integer.parseInt(waitTimematcher.group(1)));
                }
                //process captcha & password_if_required
                while (getContentAsString().contains("class=\"captcha_code\"")) {
                    stepCaptcha();
                    //checkProblems();
                }

                final HttpMethod freeMethod3 = getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromAHrefWhereATagContains(httpFile.getFileName())
                        .toGetMethod();

                if (!tryDownloadAndSaveFile(freeMethod3)) {
                    checkProblems();//if downloading failed
                    logger.warning(getContentAsString());//log the info
                    throw new PluginImplementationException();//some unknown problem
                }

            } else throw new InvalidURLOrServiceProblemException("Can't get second url");
        } else throw new InvalidURLOrServiceProblemException("Can't find download link");
    }

    private boolean isPassworded() {
        boolean passworded = getContentAsString().contains("<input type=\"password\" name=\"password\" class=\"myForm\">");
        return passworded;
    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("could not be found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        } else if (contentAsString.contains("No such file")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        } else if (contentAsString.contains("tidak ditemukan")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private void checkSecondaryProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>maknyos Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        } else if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>maknyos Error:</b><br>Currently a lot of users are downloading files."));
        } else if (contentAsString.contains("This file reached max downloads limit")) {
            throw new ServiceConnectionProblemException("This file reached max downloads limit");
        }
    }

    private String getPassword() throws Exception {
        MaknyosPasswordUI ps = new MaknyosPasswordUI();
        if (getDialogSupport().showOKCancelDialog(ps, "Secured file on maknyos")) {
            return (ps.getPassword());
        } else throw new NotRecoverableDownloadException("This file is secured with a password");
    }

    private void stepCaptcha() throws Exception {
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

        final HttpMethod postMethod = getMethodBuilder()
                .setActionFromFormWhereTagContains("download2", true)
                .setAction(fileURL)
                .setParameter("code", strCaptcha)
                .removeParameter("method_premium")
                .toPostMethod();

        if (isPassworded()) ((PostMethod) postMethod).setParameter("password", getPassword());
        if (!makeRedirectedRequest(postMethod)) {
            throw new ServiceConnectionProblemException();
        }
    }
}