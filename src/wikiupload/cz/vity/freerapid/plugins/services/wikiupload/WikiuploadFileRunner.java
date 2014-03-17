package cz.vity.freerapid.plugins.services.wikiupload;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.logging.Logger;
import java.io.IOException;

import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;

/**
 * Class which contains main code
 *
 * @author Arthur Gunawan
 */
class WikiuploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(WikiuploadFileRunner.class.getName());
    private static final String SERVICE_WEB = "http://www.wikiupload.com/";

    private String fileCommentURL;
    private String lName;
    private String ID;


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();

        fileURL = CommonURL(fileURL);


        checkName();//ok let's extract file name and size from the page

    }


    private String CommonURL(String cURL) throws ErrorDuringDownloadingException {
     
        if (cURL.contains("wikiupload.com/download_page.php")) {
            return cURL;

        } else if (cURL.contains("wikiupload.com/middle_page.php")) {
            return cURL.replaceFirst("wikiupload.com/middle_page.php", "wikiupload.com/download_page.php");

        } else if (cURL.contains("wikiupload.com/comment.php")) {
            return cURL.replaceFirst("wikiupload.com/comment.php", "wikiupload.com/download_page.php");
        } else {
            throw new PluginImplementationException("Invalid URL");
            //return null;

        }

    }


    private void checkName() throws Exception {
        fileCommentURL = fileURL;
        fileCommentURL = fileCommentURL.replaceFirst("download_page.php", "comment.php");
        final GetMethod getMethod = getGetMethod(fileCommentURL);
        if (makeRedirectedRequest(getMethod)) {
            lName = fileURL.substring(fileURL.lastIndexOf("download_page.php"));
            lName = "<td><a href='" + lName + "'><span class=\"title_6\">";
            PlugUtils.checkName(httpFile, getContentAsString(), lName, "</span></a></td>");//TODO
        } else {
            throw new PluginImplementationException();
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        fileURL = CommonURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);
        checkName();
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            String contentAsString = getContentAsString();
            ID = PlugUtils.getParameter("id", contentAsString);

            final MethodBuilder methodBuilder = getMethodBuilder();
            final String redirectURL = fileURL;

            httpMethod = stepCaptcha(redirectURL);

            if (!tryDownloadAndSaveFile(httpMethod)) {
                //checkAllProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty");
            }

        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private HttpMethod stepCaptcha(String redirectURL) throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = SERVICE_WEB + "turing_image.php";//PlugUtils.getStringBetween(getContentAsString(), "\"", "\" align=\"absmiddle\" id=\"image\" name=\"image\"");
        logger.info("Captcha URL " + captchaSrc);
        final String captcha = captchaSupport.getCaptcha(captchaSrc);

        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            //return getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("code", captcha).setParameter("id", ID).toHttpMethod();
            //logger.info("!!!!!!!!!!!!!!!! LAST CONTENT !!!!!!!!!!!!!!!!!!!" + getContentAsString());
            
            //return getMethodBuilder().setReferer(fileURL).setActionFromFormByIndex(1, true).setAction(fileURL).setParameter("code", captcha).toHttpMethod();
            return getMethodBuilder().setActionFromImgSrcWhereTagContains("turing_image.php").setAction(fileURL).setParameter("code", captcha).toHttpMethod();
            
        }


    }
}
