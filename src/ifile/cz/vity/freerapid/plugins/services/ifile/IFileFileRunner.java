package cz.vity.freerapid.plugins.services.ifile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda
 * @since 0.82
 */
class IFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(IFileFileRunner.class.getName());
    private final static String REDIRECT_URL = "http://ifile.it/dl";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).setEncodePathAndQuery(true).toHttpMethod();

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
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).setEncodePathAndQuery(true).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkNameAndSize();
            final URI fileURI = new URI(fileURL);
            final String[] filePath = fileURI.getPath().split("/");

            if (filePath.length > 1) {
                final String contentAsString = getContentAsString();
                final String redirectURL = PlugUtils.getStringBetween(contentAsString, "var url = '", "'") + filePath[1] + ",type=simple" + PlugUtils.getStringBetween(getContentAsString(), "var url = url + '", "'") + PlugUtils.getStringBetween(contentAsString, "esn='+__esn+'", "' + c;");
                httpMethod = getMethodBuilder().setReferer(REDIRECT_URL).setAction(redirectURL).toHttpMethod();

                if (makeRedirectedRequest(httpMethod)) {
                    while (getContentAsString().contains("\"message\":\"show_captcha\"")) {
                        httpMethod = stepCaptcha(redirectURL);

                        if (!makeRedirectedRequest(httpMethod)) {
                            throw new ServiceConnectionProblemException();
                        }
                    }

                    checkAllProblems();
                    httpMethod = getMethodBuilder().setReferer(REDIRECT_URL).setAction(REDIRECT_URL).toHttpMethod();

                    if (makeRedirectedRequest(httpMethod)) {
                        httpMethod = getMethodBuilder().setReferer(REDIRECT_URL).setAction(PlugUtils.getStringBetween(getContentAsString(), "href=\"", "\">Download<")).toHttpMethod();

                        if (!tryDownloadAndSaveFile(httpMethod)) {
                            checkAllProblems();
                            logger.warning(getContentAsString());
                            throw new IOException("File input stream is empty");
                        }
                    } else {
                        throw new ServiceConnectionProblemException();
                    }
                } else {
                    throw new ServiceConnectionProblemException();
                }
            } else {
                throw new PluginImplementationException("File key was not found");
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("file removed") || contentAsString.contains("no such file exists") || contentAsString.contains("file expired")) {
            throw new URLNotAvailableAnymoreException("File was not found");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("gray;\">((?:.|\\s)+?)&nbsp;\\s*\\(");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

            matcher = getMatcherAgainstContent("gray;\">(?:.|\\s)+?\\((.+?)\\)");

            if (matcher.find()) {
                final long fileSize = PlugUtils.getFileSizeFromString(matcher.group(1));
                logger.info("File size " + fileSize);
                httpFile.setFileSize(fileSize);
            } else {
                logger.warning("File size was not found");
                throw new PluginImplementationException();
            }
        } else {
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private HttpMethod stepCaptcha(String redirectURL) throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = "http://ifile.it/download:captcha";
        logger.info("Captcha URL " + captchaSrc);
        final String captcha = captchaSupport.getCaptcha(captchaSrc);

        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            return getMethodBuilder().setReferer(REDIRECT_URL).setAction(redirectURL + captcha).toHttpMethod();
        }
    }
}