package cz.vity.freerapid.plugins.services.ifile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda
 * @since 0.82
 */
class IFileFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(IFileFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
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
        GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkAllProblems();
            checkNameAndSize();
            final URI fileURI = new URI(fileURL);
            final String[] filePath = fileURI.getPath().split("/");

            if (filePath.length > 1) {
                String redirectURL = PlugUtils.getStringBetween(getContentAsString(), "var url = '", "'") + filePath[1] + ",type=simple,captcha=";
                HttpMethod httpMethod = stepCaptcha(redirectURL);

                if (makeRedirectedRequest(httpMethod)) {
                    while (getContentAsString().contains("\"message\":\"show_captcha\"")) {
                        httpMethod = stepCaptcha(redirectURL);

                        if (!makeRedirectedRequest(httpMethod)) {
                            throw new ServiceConnectionProblemException();
                        }
                    }

                    checkAllProblems();
                    client.setReferer(redirectURL);
                    redirectURL = "http://ifile.it/dl";
                    getMethod = getGetMethod(redirectURL);

                    if (makeRedirectedRequest(getMethod)) {
                        final Matcher matcher = getMatcherAgainstContent("href=\"(.+?)\">Download<");

                        if (matcher.find()) {
                            client.setReferer(redirectURL);
                            final String finalURL = matcher.group(1);
                            getMethod = getGetMethod(finalURL);

                            if (!tryDownloadAndSaveFile(getMethod)) {
                                checkAllProblems();
                                logger.warning(getContentAsString());
                                throw new IOException("File input stream is empty");
                            }
                        } else {
                            throw new PluginImplementationException("Download link was not found");
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
            return getMethodBuilder().setReferer("http://ifile.it/dl").setAction(redirectURL + captcha).toHttpMethod();
        }
    }
}