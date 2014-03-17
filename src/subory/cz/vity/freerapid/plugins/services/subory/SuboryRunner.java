package cz.vity.freerapid.plugins.services.subory;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda
 */
class SuboryRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(SuboryRunner.class.getName());
    private static final String SERVICE_WEB = "http://www.subory.sk";

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
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkAllProblems();
            checkNameAndSize();

            if (getContentAsString().contains("captcha")) {
                client.setReferer(fileURL);
                //client.getHTTPClient().getParams().setParameter("considerAsStream", "text/plain");
                PostMethod postMethod = new PostMethod();

                while (getContentAsString().contains("captcha")) {
                    postMethod = stepCaptcha();
                    makeRequest(postMethod);
                }

                if (!tryDownloadAndSaveFile(postMethod)) {
                    checkAllProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty");
                }
            } else {
                throw new PluginImplementationException("Download link was not found");
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("Neplatn. odkaz");

        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("Neplatný odkaz");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("class=\"down-filename\">(?:.|\\s)+?>\\s*(.+?)<");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

            matcher = getMatcherAgainstContent("Ve.kos. s.boru:</strong></td><td\\s*class=desc>(.+?)<");

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

    private PostMethod stepCaptcha() throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();

        final Matcher matcher = getMatcherAgainstContent("class=captcha src=\"(.+?)\"");

        if (matcher.find()) {
            final String captchaSrc = matcher.group(1);
            logger.info("Captcha URL " + captchaSrc);
            final String captcha = captchaSupport.getCaptcha(SERVICE_WEB + captchaSrc);

            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            } else {
                final PostMethod postMethod = getPostMethod(fileURL);
                postMethod.addParameter("submitted", "1");
                postMethod.addParameter("str", captcha);

                return postMethod;
            }
        } else {
            throw new PluginImplementationException("Captcha picture was not found");
        }
    }
}