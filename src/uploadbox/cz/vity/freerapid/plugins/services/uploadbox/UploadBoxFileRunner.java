package cz.vity.freerapid.plugins.services.uploadbox;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * @author Kajda
 * @since 0.82
 */
class UploadBoxFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadBoxFileRunner.class.getName());
    private final static String SERVICE_WEB = "http://www.uploadbox.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setPageEncoding("ISO-8859-1");
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
        setPageEncoding("ISO-8859-1");
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkNameAndSize();
            httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormWhereTagContains("Free Download", true).setBaseURL(SERVICE_WEB).toHttpMethod();

            if (makeRedirectedRequest(httpMethod)) {
                if (getContentAsString().contains("Enter CAPTCHA code here")) {
                    while (getContentAsString().contains("Enter CAPTCHA code here")) {
                        httpMethod = stepCaptcha();
                        makeRedirectedRequest(httpMethod);
                    }

                    checkAllProblems();
                    if (getContentAsString().contains("click here")) {
                        httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("click here").toHttpMethod();
                    } else {
                        httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormWhereTagContains("Download File", true).toGetMethod();
                    }

                    if (!tryDownloadAndSaveFile(httpMethod)) {
                        checkContentType(httpMethod);
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

        if (contentAsString.contains("File removed from service") || contentAsString.contains("File deleted from service") || contentAsString.contains("This Link Is Not Available")) {
            throw new URLNotAvailableAnymoreException("File removed from service");
        }

        if (!contentAsString.contains("File name:")) {
            throw new URLNotAvailableAnymoreException("File was not found");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("You allready download some file") || contentAsString.contains("You already download some file")) {
            throw new YouHaveToWaitException("You already download some file. Please finish download and try again", 2 * 60);
        }


    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "File name:</span>", "</p>");
        PlugUtils.checkFileSize(httpFile, contentAsString, "Size:</span>", "<span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private HttpMethod stepCaptcha() throws ErrorDuringDownloadingException, URIException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = getMethodBuilder().setBaseURL(SERVICE_WEB).setActionFromImgSrcWhereTagContains("id=\"captcha\"").toGetMethod().getURI().toString();
        logger.info("Captcha URL " + captchaSrc);
        final String captcha = captchaSupport.getCaptcha(captchaSrc);

        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            return getMethodBuilder().setReferer(fileURL).setActionFromFormByName("free", true).setBaseURL(SERVICE_WEB).setParameter("enter", captcha).toHttpMethod();
        }
    }

    private void checkContentType(HttpMethod httpMethod) throws ErrorDuringDownloadingException {
        final Header contentType = httpMethod.getResponseHeader("Content-Type");
        final String contentTypeValue = contentType.getValue().toLowerCase(Locale.ENGLISH);

        if (contentTypeValue.equals("archive/zip")) {
            client.getHTTPClient().getParams().setParameter("considerAsStream", "archive/zip");
            throw new YouHaveToWaitException("Suspicious Content-Type", 4);
        }
    }
}