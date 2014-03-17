package cz.vity.freerapid.plugins.services.netgull;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Kajda
 * @since 0.82
 */
class NetGullFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NetGullFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkSeriousProblems();
            checkNameAndSize();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkNameAndSize();

            if (getContentAsString().contains("captcha")) {
                while (getContentAsString().contains("captcha")) {
                    httpMethod = stepCaptcha();
                    makeRedirectedRequest(httpMethod);
                }

                httpMethod = getMethodBuilder().setReferer(fileURL).setAction(PlugUtils.getStringBetween(getContentAsString(), "document.location=\"", "\";")).toHttpMethod();

                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkAllProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty");
                }
            } else {
                throw new PluginImplementationException("Captcha form was not found");
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("Your requested file is not found")) {
            throw new URLNotAvailableAnymoreException("Your requested file is not found");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("AccessKey is expired")) {
            throw new YouHaveToWaitException("AccessKey is expired", 4);
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "File name:</b>&nbsp;&nbsp;&nbsp;", "<");
        PlugUtils.checkFileSize(httpFile, contentAsString, "File size:</b>&nbsp;&nbsp;&nbsp;", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private HttpMethod stepCaptcha() throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = PlugUtils.getStringBetween(getContentAsString(), "style=\"VERTICAL-ALIGN:middle\" src=\"", "\"");
        logger.info("Captcha URL " + captchaSrc);
        final String captcha = captchaSupport.getCaptcha(captchaSrc);

        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            return getMethodBuilder().setReferer(fileURL).setActionFromFormByName("myform", true).setParameter("captchacode", captcha).toHttpMethod();
        }
    }
}