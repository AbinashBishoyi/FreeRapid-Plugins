package cz.vity.freerapid.plugins.services.filefactory;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda
 */
class FileFactoryFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(FileFactoryFileRunner.class.getName());
    private static final String SERVICE_WEB = "http://www.filefactory.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkSeriousProblems();
            checkNameAndSize(getContentAsString());
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
            checkNameAndSize(getContentAsString());

            if (getContentAsString().contains("Download with FileFactory TrafficShare")) {
                HttpMethod finalMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download with FileFactory TrafficShare").toGetMethod();
                if (tryDownloadAndSaveFile(finalMethod)) {
                    return;
                }
                makeRedirectedRequest(getMethod);
            }

            final MethodBuilder methodBuilder = getMethodBuilder();
            final HttpMethod httpMethod = methodBuilder.setReferer(fileURL).setActionFromAHrefWhereATagContains("Download Now").setBaseURL(SERVICE_WEB).toGetMethod();
            final String redirectURL = SERVICE_WEB + methodBuilder.getAction();

            if (makeRedirectedRequest(httpMethod)) {
                /*
                if (getContentAsString().contains("captcha")) {
                    int captchaOCRCounter = 1;

                    while (getContentAsString().contains("captcha")) {
                        final PostMethod postMethod = stepCaptcha(redirectURL, captchaOCRCounter++);
                        makeRedirectedRequest(postMethod);
                    }

                    checkAllProblems();

                    matcher = getMatcherAgainstContent("href=\"(.+?)\" class=\"download\">CLICK HERE");

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
                    throw new PluginImplementationException("Captcha form was not found");
                }
                */

                checkAllProblems();
                HttpMethod finalMethod = getMethodBuilder().setReferer(redirectURL).setActionFromAHrefWhereATagContains("Download with FileFactory Basic").toGetMethod();
                downloadTask.sleep(PlugUtils.getWaitTimeBetween(getContentAsString(), "id=\"startWait\" value=\"", "\"", TimeUnit.SECONDS));
                if (!tryDownloadAndSaveFile(finalMethod)) {
                    checkAllProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty");
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

        if (contentAsString.contains("Sorry, this file is no longer available")) {
            throw new URLNotAvailableAnymoreException("Sorry, this file is no longer available. It may have been deleted by the uploader, or has expired");
        }

        if (contentAsString.contains("Sorry, there are currently no free download slots available on this server")) {
            throw new YouHaveToWaitException("Sorry, there are currently no free download slots available on this server", 60);
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();

        /*
        if (contentAsString.contains("Sorry, your time to enter the code has expired")) {
            throw new YouHaveToWaitException("Sorry, your time to enter the code has expired. Please try again", 60);
        }
        */

        if (contentAsString.contains("Your download slot has expired")) {
            throw new YouHaveToWaitException("Your download slot has expired.  Please try again", 60);
        }

        if (contentAsString.contains("You are currently downloading too many files at once")) {
            throw new YouHaveToWaitException("You are currently downloading too many files at once. Multiple simultaneous downloads are only permitted for Premium Members", 60);
        }

        Matcher matcher = getMatcherAgainstContent("You(?:r IP)? \\((.+?)\\) (?:has|have) exceeded the download limit for free users");

        if (matcher.find()) {
            final String userIP = matcher.group(1);

            matcher = getMatcherAgainstContent("Please wait (.+?) (.+?) to download more files");

            int waitSeconds = 2 * 60;

            if (matcher.find()) {
                if (matcher.group(2).equals("minutes")) {
                    waitSeconds = 60 * Integer.parseInt(matcher.group(1));
                } else {
                    waitSeconds = Integer.parseInt(matcher.group(1));
                }
            }

            throw new YouHaveToWaitException(String.format("You (%s) have exceeded the download limit for free users", userIP), waitSeconds);
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "class=\"last\">", "</span");
        PlugUtils.checkFileSize(httpFile, content, "<span>", "file uploaded");
    }

    /*
    private PostMethod stepCaptcha(String redirectURL, int captchaOCRCounter) throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();

        final Matcher matcher = getMatcherAgainstContent("class=\"captchaImage\" src=\"(.+?)\"");

        if (matcher.find()) {
            final String captchaSrc = SERVICE_WEB + matcher.group(1);
            logger.info("Captcha URL " + captchaSrc);
            final String captcha;

            if (captchaOCRCounter <= 0) {
                captcha = readCaptchaImage(captchaSrc);
            } else {
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

    private String readCaptchaImage(String captchaSrc) throws ErrorDuringDownloadingException {
        final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(captchaSrc);
        final BufferedImage croppedCaptchaImage = captchaImage.getSubimage(1, 1, captchaImage.getWidth() - 2, captchaImage.getHeight() - 2);
        String captcha = PlugUtils.recognize(croppedCaptchaImage, "-C A-z-0-9");

        if (captcha != null) {
            logger.info("Captcha - OCR recognized " + captcha);
        } else {
            captcha = "";
        }

        captchaImage.flush();

        return captcha;
    }
    */
}