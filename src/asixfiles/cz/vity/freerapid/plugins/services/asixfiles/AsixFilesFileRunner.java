package cz.vity.freerapid.plugins.services.asixfiles;

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
class AsixFilesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AsixFilesFileRunner.class.getName());
    //private boolean badConfig = false;

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
        String nameAndSizeRule = "You have requested.*?http://(?:www\\.)?" + "asixfiles.com" + "/[a-z0-9]{12}/(.*)</font> \\((.*?)\\)</font>$";
        Matcher matcher = PlugUtils.matcher(nameAndSizeRule, content);
        if (matcher.find()) {
            httpFile.setFileName(matcher.group(1));
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));

            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else throw new ErrorDuringDownloadingException();
    }

    private void login() throws Exception {
        synchronized (AsixFilesFileRunner.class) {
            AsixFilesServiceImpl service = (AsixFilesServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();

            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No AsixFiles registered account login information!");
                }
                //badConfig = false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction("http://www.asixfiles.com/login.html")
                    .setParameter("op", "login")
                    .setParameter("redirect", "")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("submit", "")
                    .toPostMethod();
            addCookie(new Cookie(".asixfiles.com", "login", pa.getUsername(), "/", null, false));
            addCookie(new Cookie(".asixfiles.com", "xfss", "", "/", null, false));
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");

            if (getContentAsString().contains("Incorrect Login or Password"))
                throw new NotRecoverableDownloadException("Invalid AsixFiles registered account login information!");
        }
    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (PlugUtils.matcher("No such file|File not found|File Not Found", contentAsString).find()) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("This file reached max downloads limit")) {
            throw new PluginImplementationException("This file reached max downloads limit");
        }
        if (contentAsString.contains("You have reached the download-limit")) {
            throw new PluginImplementationException("You have reached the download-limit");
        }
    }

    private boolean isPassworded() {
        boolean passworded = getContentAsString().contains("<input type=\"password\" name=\"password\" class=\"myForm\">");
        return passworded;
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Login");
        login();
        String contentAsString = getContentAsString();//check for response

        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            //free download
            HttpMethod freeMethod = getMethodBuilder().setAction(fileURL).setBaseURL(fileURL).setReferer(fileURL).setActionFromFormWhereTagContains("Free Download", true).removeParameter("method_premium").toHttpMethod();
            if (makeRedirectedRequest(freeMethod)) {
                //process captcha
                logger.info("Processing captcha");
                contentAsString = getContentAsString();
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
                String strCaptcha = Integer.toString(Integer.parseInt(strbuffCaptcha.toString()));
                logger.info("Captcha : " + strCaptcha);

                //process wait time
                String waitTimeRule = "id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span";
                Matcher waitTimematcher = PlugUtils.matcher(waitTimeRule, contentAsString);
                if (waitTimematcher.find()) {
                    downloadTask.sleep(Integer.parseInt(waitTimematcher.group(1)));
                }

                 final MethodBuilder methodBuilder = getMethodBuilder()
                         .setActionFromFormByName("F1", true)
                         .setAction(fileURL)
                         .setParameter("code", strCaptcha)
                         .removeParameter("method_premium");

                if (isPassworded()) {
                    final String password = getDialogSupport().askForPassword("AsixFiles");
                    if (password == null) {
                        throw new NotRecoverableDownloadException("This file is secured with a password");
                    }
                    methodBuilder.setParameter("password", password);
                }

                final HttpMethod freeMethod2 = methodBuilder.toPostMethod();

                if (!tryDownloadAndSaveFile(freeMethod2)) {
                    checkProblems();//if downloading failed
                    logger.warning(getContentAsString());//log the info
                    throw new PluginImplementationException();//some unknown problem
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


}