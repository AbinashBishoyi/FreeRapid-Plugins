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
 * @author JPEXS
 */
class HellSpyFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(HellSpyFileRunner.class.getName());
    private final static String SERVICE_WEB = "http://www.hellspy.com/";
    private static List<Cookie> cookies;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL();
        makeRedirectedRequest(getGetMethod(SERVICE_WEB));
        setLanguage();
        
        final GetMethod getMethod = getGetMethod(fileURL);
        
        if (makeRedirectedRequest(getMethod)) {                      
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

            if (getContentAsString().contains(">Login</a>")) {
                throw new BadLoginException("Failed to log in");
            }            
            HttpMethod httpMethod=getMethodBuilder().setAction(fileURL+"?download=1").toGetMethod();                        
            if (makeRedirectedRequest(httpMethod)) {           
                if(getContentAsString().contains("dostatek kreditu")){
                   throw new NotRecoverableDownloadException("No credit for download this file!");
                }
                httpMethod=getMethodBuilder().setActionFromTextBetween("launchFullDownload('", "'").toGetMethod();
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
        String content = getContentAsString();
        int p1=content.indexOf("<span class=\"filesize");
        if(p1==-1){
           throw new PluginImplementationException();
        }
        content=content.substring(p1);
        int p2=content.indexOf("</h1>");
        if(p2==-1){
           throw new PluginImplementationException();
        }
        content=content.substring(0,p2+5);        
        PlugUtils.checkName(httpFile, content, "<h1>", "</h1>");
        final String size = PlugUtils.getStringBetween(content, "right\">", "</span></span>").replace("<span>", "");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void setLanguage() throws Exception {
        String phpsess=getCookieByName("PHPSESSID").getValue();
        Matcher m=Pattern.compile("http://([a-z]+?\\.)?hellspy\\.([a-z]{2,3})/(.+)").matcher(fileURL);
        if(m.find()){
            final HttpMethod httpMethod = getMethodBuilder()
                .setReferer(SERVICE_WEB)
                .setAction("http://"+m.group(1)+"hellspy.com/--"+phpsess+"-/")
                //.setActionFromAHrefWhereATagContains("English")
                .toGetMethod();
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException();
        }
    }

    private void login() throws Exception {
        synchronized (HellSpyFileRunner.class) {
            if (cookies != null) {
                for(Cookie c:cookies){
                    addCookie(c);
                }
                HttpMethod httpMethod = getGetMethod(SERVICE_WEB);
                if (!makeRedirectedRequest(httpMethod))
                    throw new ServiceConnectionProblemException();
                setLanguage();
               httpMethod = getGetMethod(SERVICE_WEB);
                if (!makeRedirectedRequest(httpMethod))
                    throw new ServiceConnectionProblemException();
                if(!getContentAsString().contains("Logout")){
                    cookies=null;
                    logger.info("Session timed out, reloging");
                }
            }
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
              
                httpMethod = getMethodBuilder()
                        .setReferer(SERVICE_WEB)
                        .setAction("http://www.hell-share.com/user/login/?do=apiLoginForm-submit&api_hash=hellspy_iq&user_hash="+getCookieByName("PHPSESSID").getValue())
                        .setParameter("username", pa.getUsername())
                        .setParameter("password",pa.getPassword())
                        .setParameter("permanent_login", "on")
                        .setParameter("submit_login", "Login")
                        .setParameter("login", "1")
                        .setParameter("redir_url", "http://www.hellspy.com/?do=loginBox-login")
                        .toPostMethod();
                if (!makeRedirectedRequest(httpMethod))
                    throw new ServiceConnectionProblemException("Error posting login info");

                if (getContentAsString().contains("Error while logging in"))
                    throw new BadLoginException("Invalid HellSpy login information!");
                cookies=new ArrayList<Cookie>();
                //There are 3 PHPSESSID cookies, save each of them...
                for (final Cookie cookie : client.getHTTPClient().getState().getCookies()) {
                    cookies.add(cookie);
                }
            }

            

        }
    }


}