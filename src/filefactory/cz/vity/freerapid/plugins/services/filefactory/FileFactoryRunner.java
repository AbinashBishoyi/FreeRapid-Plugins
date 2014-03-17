package cz.vity.freerapid.plugins.services.filefactory;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.io.IOException;
import java.awt.image.BufferedImage;

/**
 * @author Kajda
 */
class FileFactoryFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileFactoryFileRunner.class.getName());
    private final static String SERVICE_WEB = "http://www.filefactory.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
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
            checkProblems();
            checkNameAndSize();

            Matcher matcher = getMatcherAgainstContent("class=\"download\" href=\"(.+?)\"");

            if (matcher.find()) {
                String redirectURL = SERVICE_WEB + matcher.group(1);
                client.setReferer(redirectURL);
                getMethod = getGetMethod(redirectURL);

                if (makeRedirectedRequest(getMethod)) {
                    if (getContentAsString().contains("captcha")) {
                        int captchaOCRCounter = 1;

                        while (getContentAsString().contains("captcha")) {
                            final PostMethod postMethod = stepCaptcha(redirectURL, captchaOCRCounter++);
                            makeRedirectedRequest(postMethod);
                        }

                        checkProblems();

                        matcher = getMatcherAgainstContent("href=\"(.+?)\" class=\"download\">CLICK HERE");

                        if (matcher.find()) {
                            final String finalURL = matcher.group(1);
                            client.setReferer(finalURL);
                            getMethod = getGetMethod(finalURL);

                            if (!tryDownloadAndSaveFile(getMethod)) {
                                checkProblems();
                                logger.warning(getContentAsString());
                                throw new IOException("File input stream is empty");
                            }
                        }
                        else {
                            throw new PluginImplementationException("Download link was not found");
                        }
                    }
                    else {
                        throw new PluginImplementationException("Captcha form was not found");
                    }
                }
                else {
                    throw new ServiceConnectionProblemException();
                }
            }
            else {
                throw new PluginImplementationException("Redirect link was not found");
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        Matcher matcher;

        if (contentAsString.contains("Sorry, this file is no longer available")) {
            throw new URLNotAvailableAnymoreException("Sorry, this file is no longer available. It may have been deleted by the uploader, or has expired");
        }

        if (contentAsString.contains("Sorry, your time to enter the code has expired")) {
            throw new YouHaveToWaitException("Sorry, your time to enter the code has expired. Please try again", 60);
        }

        if (contentAsString.contains("Sorry, there are currently no free download slots available on this server")) {
            throw new YouHaveToWaitException("Sorry, there are currently no free download slots available on this server", 60);
        }

        if (contentAsString.contains("You are currently downloading too many files at once")) {
            throw new YouHaveToWaitException("You are currently downloading too many files at once. Multiple simultaneous downloads are only permitted for Premium Members", 60);
        }

        matcher = getMatcherAgainstContent("Your IP \\((.+?)\\) has exceeded the download limit for free users");

        if (matcher.find()) {
            final String userIP = matcher.group(1);

            matcher = getMatcherAgainstContent("Please wait (.+?) (.+?) to download more files");

            int waitSeconds = 2 * 60;

            if (matcher.find()) {
                if (matcher.group(2).equals("minutes")) {
                    waitSeconds = 60 * Integer.parseInt(matcher.group(1));
                }
                else {
                    waitSeconds = Integer.parseInt(matcher.group(1));
                }
            }

            throw new YouHaveToWaitException(String.format("Your IP %s has exceeded the download limit for free users", userIP), waitSeconds);
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("class=\"last\">(.+?)<");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

            matcher = getMatcherAgainstContent("<span>(.+?) file uploaded");

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

    private PostMethod stepCaptcha(String redirectURL, int captchaOCRCounter) throws Exception {
        CaptchaSupport captchaSupport = getCaptchaSupport();

        Matcher matcher = getMatcherAgainstContent("class=\"captchaImage\" src=\"(.+?)\"");

        if (matcher.find()) {
            final String captchaSrc = SERVICE_WEB + matcher.group(1);
            logger.info("Captcha URL " + captchaSrc);
            String captcha;

            if (captchaOCRCounter <= 0) { // TODO
                captcha = readCaptchaImage(captchaSrc);
            }
            else {
                captcha = captchaSupport.getCaptcha(captchaSrc);
            }

            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            } else {
                final PostMethod postMethod = getPostMethod(redirectURL);
                postMethod.addParameter("captchaText", captcha);

                return postMethod;
            }
        } else {
            throw new PluginImplementationException("Captcha picture was not found");
        }
    }

    private String readCaptchaImage(String captchaSrc) throws Exception {
        final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(captchaSrc);
        final BufferedImage croppedCaptchaImage = captchaImage.getSubimage(1, 1, captchaImage.getWidth() - 2, captchaImage.getHeight() - 2);
        String captcha = PlugUtils.recognize(croppedCaptchaImage, "-C A-z-0-9");

        if (captcha != null) {
            logger.info("Captcha - OCR recognized " + captcha);
        }
        else {
            captcha = "";
        }

        captchaImage.flush();

        return captcha;
    }
}