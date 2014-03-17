package cz.vity.freerapid.plugins.services.mega1280;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class Mega1280FileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Mega1280FileRunner.class.getName());
    private int captchaCounter;


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<span class=\"clr05\"><b>", "</b></span><br />");
        PlugUtils.checkFileSize(httpFile, content, "<strong>", "</strong>\n</td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            captchaCounter = 0;
            final String firstPage = "name=\"code_security\" id=\"code_security\"";
            while (getContentAsString().contains(firstPage)) {
                final HttpMethod httpMethod = stepCaptcha();
                //here is the download link extraction
                if (!makeRedirectedRequest(httpMethod)) //reload captcha page or move to another page
                    throw new PluginImplementationException();
            }
            if (!getContentAsString().contains("hddomainname"))
                throw new YouHaveToWaitException("Invalid page content", 30);
            final String hddomainname = PlugUtils.getStringBetween(getContentAsString(), "hddomainname\" style=\"display:none\">", "</div>");
            final String hdfolder = PlugUtils.getStringBetween(getContentAsString(), "hdfolder\" style=\"display:none\">", "</div>");
            final String hdcode = PlugUtils.getStringBetween(getContentAsString(), "hdcode\" style=\"display:none\">", "</div>");
            final String hdfilename = PlugUtils.getStringBetween(getContentAsString(), "hdfilename\" style=\"display:none\">", "</div>");
            downloadTask.sleep(2);
            final HttpMethod httpMethod = getMethodBuilder().setAction(hddomainname + hdfolder + hdcode + "/" + hdfilename).toGetMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found") || contentAsString.contains("Li\u00EAn k\u1EBFt b\u1EA1n v\u1EEBa ch\u1ECDn kh\u00F4ng t\u1ED3n t\u1EA1i tr\u00EAn h\u1EC7 th\u1ED1ng") || contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private HttpMethod stepCaptcha() throws Exception {
        CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaURL = "http://mega.1280.com/security_code.php";
        final String captcha;
        if (captchaCounter < 6) {
            ++captchaCounter;
            final BufferedImage captchaImage = captchaSupport.getCaptchaImage(captchaURL);
            captcha = PlugUtils.recognize(captchaImage, "-d -1 -C A-z-0-9");
            logger.info("captcha = " + captcha);
        } else {
            captcha = captchaSupport.getCaptcha(captchaURL);
        }

        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            return getMethodBuilder().setReferer(fileURL).setActionFromFormByName("frm_download", true).setAction(fileURL).setParameter("code_security", captcha.toLowerCase(Locale.ENGLISH)).toPostMethod();
        }


    }

}