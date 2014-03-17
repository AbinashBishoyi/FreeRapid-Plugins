package cz.vity.freerapid.plugins.services.bitroad;

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
class BitRoadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BitRoadFileRunner.class.getName());

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

            Matcher matcher = getMatcherAgainstContent("form action=\"(.+?)\"");

            if (matcher.find()) {
                final String redirectURL = matcher.group(1);
                client.setReferer(redirectURL);
                PostMethod postMethod = getPostMethod(redirectURL);

                matcher = getMatcherAgainstContent("name=\"uid\" value=\"(.+?)\"");

                if (matcher.find()) {
                    final String paramUid = matcher.group(1);
                    postMethod.addParameter("uid", paramUid);

                    if (makeRedirectedRequest(postMethod)) {
                        if (getContentAsString().contains("name='cap'")) {
                            int captchaOCRCounter = 1;

                            while (getContentAsString().contains("name='cap'")) {
                                postMethod = stepCaptcha(redirectURL, paramUid, captchaOCRCounter++);
                                makeRedirectedRequest(postMethod);
                            }

                            matcher = getMatcherAgainstContent("href='(.+?)' title='Your link to download file'");

                            if (matcher.find()) {
                                final String finalURL = matcher.group(1);
                                client.setReferer(finalURL);
                                getMethod = getGetMethod(finalURL);
                                downloadTask.sleep(4);

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
                    throw new PluginImplementationException("Parameter 'uid' was not found");
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
        //final String contentAsString = getContentAsString();

        final Matcher matcher = getMatcherAgainstContent("class=\"style7\">File <b>(.+?)</b> not found<");

        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("File %s not found", matcher.group(1)));
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("File:<b style=\"padding-left:5px;\">(.+?)<");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

            matcher = getMatcherAgainstContent("Size:<b style=\"padding-left:5px;\">(.+?)<");

            if (matcher.find()) {
                final long fileSize = PlugUtils.getFileSizeFromString(matcher.group(1));
                logger.info("File size " + fileSize);
                httpFile.setFileSize(fileSize);
            } else {
                checkProblems();
                logger.warning("File size was not found");
                throw new PluginImplementationException();
            }
        } else {
            checkProblems();
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
    
    private PostMethod stepCaptcha(String redirectURL, String paramUid, int captchaOCRCounter) throws Exception {
        CaptchaSupport captchaSupport = getCaptchaSupport();

        Matcher matcher = getMatcherAgainstContent("img src='(.+?)' border='0'");

        if (matcher.find()) {
            String captchaSrc = matcher.group(1);
            logger.info("Captcha URL " + captchaSrc);
            String captcha;

            if (captchaOCRCounter <= 3) {
                captcha = readCaptchaImage(captchaSrc);
            }
            else {
                captcha = captchaSupport.getCaptcha(captchaSrc);
            }
            
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            } else {
                final PostMethod postMethod = getPostMethod(redirectURL);
                postMethod.addParameter("uid", paramUid);
                postMethod.addParameter("cap", captcha);

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