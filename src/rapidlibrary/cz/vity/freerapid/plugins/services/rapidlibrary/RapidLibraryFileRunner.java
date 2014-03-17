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
    private int captchaCounter = 1, captchaMax = 3;


    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
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
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);

            HttpMethod httpMethod;
            if (getContentAsString().contains("Please ENTER CODE")) {
                while (getContentAsString().contains("Please ENTER CODE")) {
                    httpMethod = stepCaptcha();
                    if (!makeRedirectedRequest(httpMethod)) {
                        throw new ServiceConnectionProblemException();
                    }
                }
            } else {
                throw new PluginImplementationException("Captcha not found");
            }
            logger.info("Captcha correct");

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
        if (contentAsString.contains("file not found") || contentAsString.contains("Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private HttpMethod stepCaptcha() throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = "http://rapidlibrary.com/code2.php";
        //logger.info("Captcha URL " + captchaSrc);

        String captcha = null;
        if (captchaCounter <= captchaMax) {
            captcha = PlugUtils.recognize(captchaSupport.getCaptchaImage(captchaSrc), "-d -1 -C A-Z");
            logger.info("OCR attempt " + captchaCounter + " of " + captchaMax + ", recognized " + captcha);
            captchaCounter++;
        } else {
            logger.info("Giving up automatic recognition");
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
        }

        return getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("c_code", captcha.toUpperCase()).setParameter("act", " Download ").toPostMethod();
    }

}