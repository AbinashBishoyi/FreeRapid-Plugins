package cz.vity.freerapid.plugins.services.fileshost;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class FilesHostFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilesHostFileRunner.class.getName());
    private String captcha, password;
    private int captchaCounter = 1, captchaMax = 5;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        client.getHTTPClient().getState().addCookie(new Cookie(".fileshost.com", "yab_mylang", "en", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "File name:</b></td>\n       <td align=left width=150px>", "</td>");
        PlugUtils.checkFileSize(httpFile, content, "File size:</b></td>\n       <td align=left>", "</td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        client.getHTTPClient().getState().addCookie(new Cookie(".fileshost.com", "yab_mylang", "en", "/", 86400, false));
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());

            if (getContentAsString().contains("captcha")) {
                while (getContentAsString().contains("captcha")) {
                    if (getContentAsString().contains("Password:")) {
                        stepPassword();
                        httpMethod = getMethodBuilder()
                                .setReferer(fileURL)
                                .setActionFromFormByName("myform", true)
                                .setAndEncodeParameter("captchacode", captcha)
                                .setAndEncodeParameter("downloadpw", password)
                                .toPostMethod();
                    } else {
                        stepCaptcha();
                        httpMethod = getMethodBuilder()
                                .setReferer(fileURL)
                                .setActionFromFormByName("myform", true)
                                .setAndEncodeParameter("captchacode", captcha)
                                .toPostMethod();
                    }
                    if (!makeRedirectedRequest(httpMethod)) {
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();
                }
                logger.info("Captcha OK");

                httpMethod = getMethodBuilder().setReferer(fileURL).setAction(PlugUtils.getStringBetween(getContentAsString(), "document.location=\"", "\";")).toGetMethod();
                client.getHTTPClient().getParams().setParameter("dontUseHeaderFilename", true);//sometimes wrong filename is reported
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty");
                }

            } else {
                throw new PluginImplementationException("Captcha form not found");
            }
        } else {
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Your requested file is not found") || contentAsString.contains("Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("You have got max allowed download sessions")) {
            throw new YouHaveToWaitException("You have got max allowed download sessions", 5 * 60);
        }
    }

    private void stepCaptcha() throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = "http://www.fileshost.com/captcha.php";
        //logger.info("Captcha URL " + captchaSrc);

        String tmpCaptcha;
        if (captchaCounter <= captchaMax) {
            tmpCaptcha = PlugUtils.recognize(captchaSupport.getCaptchaImage(captchaSrc), "-d -1 -C 0-9");
            logger.info("OCR attempt " + captchaCounter + " of " + captchaMax + ", recognized " + tmpCaptcha);
            captchaCounter++;
            if (captchaCounter > captchaMax) logger.info("Giving up automatic recognition");
        } else {
            tmpCaptcha = captchaSupport.getCaptcha(captchaSrc);
            if (tmpCaptcha == null) throw new CaptchaEntryInputMismatchException();
        }

        captcha = tmpCaptcha;
    }

    private void stepPassword() throws Exception {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("Captcha number error")) {
            stepCaptcha();
        } else if (contentAsString.contains("Password Error")) {
            getPassword();
        } else {
            getPassword();
            stepCaptcha();
        }
    }

    private void getPassword() throws Exception {
        FilesHostPasswordUI ps = new FilesHostPasswordUI();
        if (getDialogSupport().showOKCancelDialog(ps, "Secured file on FilesHost")) {
            password = ps.getPassword();
        } else throw new NotRecoverableDownloadException("This file is secured with a password");
    }

}