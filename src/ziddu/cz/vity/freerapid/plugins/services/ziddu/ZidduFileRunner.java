package cz.vity.freerapid.plugins.services.ziddu;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Kajda
 * @since 0.82
 */
class ZidduFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(ZidduFileRunner.class.getName());
    private static final String SERVICE_WEB = "http://downloads.ziddu.com";

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
            MethodBuilder methodBuilder = getMethodBuilder();
            httpMethod = methodBuilder.setReferer(fileURL).setActionFromFormByName("dfrm", true).toHttpMethod();
            final String redirectURL = methodBuilder.getAction();

            if (makeRedirectedRequest(httpMethod)) {
                if (getContentAsString().contains("name=\"securefrm\"")) {
                    if (!tryDownloadAndSaveFile(stepCaptcha(redirectURL))) {
                        checkAllProblems();
                        logger.warning(getContentAsString());
                        throw new IOException("File input stream is empty");
                    }
                } else {
                    throw new PluginImplementationException("Captcha form was not found");
                }
            } else {
                throw new ServiceConnectionProblemException();
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("Please Enter Correct Verification Code")) {
            throw new YouHaveToWaitException("Please Enter Correct Verification Code", 4);
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "class=\"fontfamilyverdana textblue14\">", "<");
        PlugUtils.checkFileSize(httpFile, contentAsString, "td height=\"18\" align=\"left\" class=\"fontfamilyverdana normal12blue\"><span class=\"fontfamilyverdana normal12black\">", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private HttpMethod stepCaptcha(String redirectURL) throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = SERVICE_WEB + PlugUtils.getStringBetween(getContentAsString(), "\"", "\" align=\"absmiddle\" id=\"image\" name=\"image\"");
        logger.info("Captcha URL " + captchaSrc);
        final String captcha = captchaSupport.getCaptcha(captchaSrc);

        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            return getMethodBuilder().setReferer(redirectURL).setActionFromFormByName("securefrm", true).setBaseURL(SERVICE_WEB).setParameter("securitycode", captcha).toHttpMethod();
        }
    }
}