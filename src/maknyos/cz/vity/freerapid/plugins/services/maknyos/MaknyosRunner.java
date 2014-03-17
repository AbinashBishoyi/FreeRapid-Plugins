package cz.vity.freerapid.plugins.services.maknyos;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
//import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.Header;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zid
 */
class MaknyosRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MaknyosRunner.class.getName());

    private void setEncoding() {
        //client.getHTTPClient().getParams().setParameter("pageCharset", "Windows-1250");
        //client.getHTTPClient().getParams().setHttpElementCharset("Windows-1250");
        client.getHTTPClient().getParams().setHttpElementCharset("utf-8");
        client.getHTTPClient().getParams().setParameter("pageCharset", "utf-8");
    }

    private boolean login() throws Exception {
        synchronized (MaknyosRunner.class) {
            final MaknyosServiceImpl service = (MaknyosServiceImpl) getPluginService();
            final PremiumAccount pa = service.getConfig();
            if (pa == null || !pa.isSet()) {
                logger.info("No account data set, skipping login");
                return false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction("/login.html")
                    .setParameter("op", "login")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");
            logger.info(getContentAsString());
            if (getContentAsString().contains("Login dan Kata sandi tidak tepat"))
                throw new BadLoginException("Invalid account login information!");
            if (getContentAsString().contains("Incorrect Login or Password"))
                throw new BadLoginException("Invalid account login information!");

            return true;
        }
    }


    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setEncoding();
        final GetMethod getMethod = getGetMethod(fileURL);
        makeRedirectedRequest(getMethod);
        checkNameandSize(getContentAsString());
    }

    @Override
    public void run() throws Exception {
        super.run();
        setEncoding();
        logger.info("Starting download in TASK " + fileURL);
        //addCookie(new Cookie(".maknyos.com", "l", "en", "/", 86400, false));

        final boolean loggedIn = login();

        final GetMethod method1 = getGetMethod(fileURL);
        if (makeRedirectedRequest(method1)) {
            String contentAsString = getContentAsString();
            checkNameandSize(contentAsString);
            final HttpMethod method2 = getMethodBuilder().setReferer(fileURL).setActionFromFormByIndex(2, true).setParameter("method_premium", "").setAction(fileURL).toPostMethod();
            if (makeRedirectedRequest(method2)) {
                if (getContentAsString().contains("This file reached max downloads limit.")) {
                    throw new InvalidURLOrServiceProblemException("This file reached max downloads limit.");
                }

                String content = getContentAsString();
                if (content.contains("<h3>Download File</h3>")) {
                    int waittime;
                    try {
                        waittime = PlugUtils.getNumberBetween(content, "\">", "</span> seconds</span>");
                    } catch (Exception ex) {
                        waittime = PlugUtils.getNumberBetween(content, "\">", "</span> detik</span>");
                    }
                    downloadTask.sleep(waittime);
                    while (getContentAsString().contains("class=\"captcha_code\"")) {
                        stepCaptcha();
                        //checkProblems();
                    }
                    Matcher matcher2 = getMatcherAgainstContent("<span style=\"background:#f9f9f9;border:1px dotted #bbb;padding:7px;\">\n" +
                            "<a href=\"(http.*)\">");
                    if (matcher2.find()) {
                        String secondUrl = matcher2.group(1);
                        final HttpMethod method3 = getMethodBuilder().setReferer(fileURL).setAction(secondUrl).toGetMethod();
                        // handle cross redirect
                        if (!tryDownloadAndSaveFile(method3)) {
                            checkProblems();
                            //logger.warning(getContentAsString());
                            throw new IOException("Download failed");
                        }
                    }
                } else throw new InvalidURLOrServiceProblemException("Can't get third url");
            } else throw new InvalidURLOrServiceProblemException("Can't get second url");
        } else throw new InvalidURLOrServiceProblemException("Can't find download link");
    }

    private boolean isPassworded() {
        boolean passworded = getContentAsString().contains("<input type=\"password\" name=\"password\" class=\"myForm\">");
        return passworded;
    }

    private void checkNameandSize(String content) throws Exception {

        if (!content.contains("maknyos.com")) {
            //logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (content.contains("File doesn")) {
            throw new URLNotAvailableAnymoreException("<b>maknyos error:</b><br>File doesn't exist");
        }
        PlugUtils.checkName(httpFile, content, "<h2>Download File ", "</h2>");
        PlugUtils.checkFileSize(httpFile, content, "(", ")</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

    }


    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>maknyos Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>maknyos Error:</b><br>Currently a lot of users are downloading files."));
        }
    }

    private String getPassword() throws Exception {
        MaknyosPasswordUI ps = new MaknyosPasswordUI();
        if (getDialogSupport().showOKCancelDialog(ps, "Secured file on maknyos")) {
            return (ps.getPassword());
        } else throw new NotRecoverableDownloadException("This file is secured with a password");
    }

    private void stepCaptcha() throws Exception {
        //Matcher matcher1 = getMatcherAgainstContent("<span style='position:absolute;padding-left:\\d+px;padding-top:\\d+px;'>&#\\d+;</span><span style='position:absolute;padding-left:\\d+px;padding-top:\\d+px;'>&#\\d+;</span><span style='position:absolute;padding-left:\\d+px;padding-top:\\d+px;'>&#\\d+;</span><span style='position:absolute;padding-left:\\d+px;padding-top:\\d+px;'>&#\\d+;</span></div>");
        Matcher matcher = getMatcherAgainstContent("<span style='position:absolute;padding-left:\\d+px;padding-top:\\d+px;'>&#\\d+;</span>");
        int numcode = 0;
        char code[] = "0000".toCharArray();
        while (matcher.find() && numcode < 4) {
            int pl = PlugUtils.getNumberBetween(matcher.group(), "padding-left:", "px");
            int num = PlugUtils.getNumberBetween(matcher.group(), "&#", ";</span>");
            int pos;
            if (pl < 16) pos = 0;
            else if (pl < 32) pos = 1;
            else if (pl < 48) pos = 2;
            else pos = 3;
            code[pos] = (char) num;
            numcode++;
        }
        String codestr = String.copyValueOf(code);
        logger.info("Code : " + codestr);
        HttpMethod postMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormByName("F1", true)
                .setParameter("code", codestr)
                .setAction(fileURL)
                .toPostMethod();
        if (isPassworded()) ((PostMethod) postMethod).setParameter("password", getPassword());
        if (!makeRedirectedRequest(postMethod)) {
            throw new ServiceConnectionProblemException();
        }
    }

}