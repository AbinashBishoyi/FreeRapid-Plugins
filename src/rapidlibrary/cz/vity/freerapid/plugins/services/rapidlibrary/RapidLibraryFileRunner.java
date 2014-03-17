package cz.vity.freerapid.plugins.services.rapidlibrary;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class RapidLibraryFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RapidLibraryFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        //these are a bit messy
        PlugUtils.checkName(httpFile, content, "File&nbsp;name:</td><td class=zae3><font color=\"#0374F1\"><b>", "</b>");
        String size = PlugUtils.getStringBetween(content, "Size:</td><td class=zae3>", "M    </td>");
        PlugUtils.getFileSizeFromString(size + "MB");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting run task " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //make the main request
            String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            if (contentAsString.contains("Download from rapidshare.com")) {
                logger.info("Captcha not found, skipping");
            } else {
                while (true) { //dummy loop for captcha
                    HttpMethod httpMethod = stepCaptcha(fileURL);
                    if (!makeRedirectedRequest(httpMethod)) {
                        checkProblems();//if failed
                        logger.warning(getContentAsString());//log the info
                        throw new PluginImplementationException();//some unknown problem
                    }
                    contentAsString = getContentAsString();
                    if (!contentAsString.contains("Please ENTER CODE")) {
                        logger.info("Captcha OK!");
                        break;//continue
                    }
                }
                logger.info("Captcha failed, retrying");
            }

            contentAsString = getContentAsString();
            checkProblems();
            String rsUrl = PlugUtils.getStringBetween(contentAsString, "<img src=\"download.png\" border=\"0\">&nbsp;<a href=\"", "\"><b><font");
            logger.info("RapidShare URL: " + rsUrl);
            //redirect to RapidShare plugin
            this.httpFile.setNewURL(new URL(rsUrl));
            this.httpFile.setPluginID("");
            this.httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let user know in FRD
        }
    }

    private HttpMethod stepCaptcha(String fileURL) throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = "http://rapidlibrary.com/code2.php";

        logger.info("Captcha URL " + captchaSrc);
        final String captcha = captchaSupport.getCaptcha(captchaSrc);

        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            //the next line... just can't get it to work
            return getMethodBuilder().setReferer(fileURL).setActionFromFormWhereTagContains("Please ENTER CODE", true).setAction(fileURL).toPostMethod();//setParameter("c_code", captcha)
            //keeps saying "form has no defined action attribute" which is absolutely true. that's the problem.
        }
    }
}