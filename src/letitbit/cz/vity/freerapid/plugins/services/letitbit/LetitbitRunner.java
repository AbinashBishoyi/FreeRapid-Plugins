package cz.vity.freerapid.plugins.services.letitbit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class LetitbitRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LetitbitRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkNameAndSize(String contentAsString) throws Exception {
        if (!contentAsString.contains("letitbit")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (contentAsString.contains("file was not found")) {
            throw new URLNotAvailableAnymoreException(String.format("The requested file was not found"));

        }
        Matcher matcher = PlugUtils.matcher("span> (.*? .b)</h1>", contentAsString);
        if (matcher.find()) {
            logger.info("File size " + matcher.group(1));
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
        }
        matcher = PlugUtils.matcher("File::</span>\\s*([^<]*)", contentAsString);
        if (matcher.find()) {
            final String fn = matcher.group(1);
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else logger.warning("File name was not found" + contentAsString);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
            int captchas = 0;
            while (captchas++ < 5) {
                Matcher matcher = Pattern.compile("form action=\"([^\"]*download[^\"]*)\"(.*)</form>", Pattern.MULTILINE | Pattern.DOTALL).matcher(getContentAsString());
                if (!matcher.find()) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new InvalidURLOrServiceProblemException("Free download link not found");
                }
                String s = matcher.group(1);
                String form = matcher.group(2);
                logger.info("Submit form to - " + s);
                client.setReferer(fileURL);
                final PostMethod postMethod = getPostMethod(s);
                PlugUtils.addParameters(postMethod, form, new String[]{"uid", "frameset", "uid2"});
                postMethod.addParameter("cap", readCaptchaImage());
                postMethod.addParameter("fix", "1");

                if (!makeRequest(postMethod)) {
                    logger.info(getContentAsString());
                    throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
                }
                matcher = PlugUtils.matcher("src=\"([^?]*)\\?link=([^\"]*)\"", getContentAsString());
                if (matcher.find()) {
                    String t = matcher.group(2);
                    logger.info("Download URL: " + t);
                    downloadTask.sleep(4);
                    logger.info(getContentAsString());
                    httpFile.setState(DownloadState.GETTING);
                    client.setReferer(matcher.group(1) + "?link=" + t);
                    final GetMethod method = getGetMethod(t);
                    method.setFollowRedirects(true);
                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        logger.info(getContentAsString());
                        throw new IOException("File input stream is empty.");
                    } else {
                        break;
                    }
                } else {
                    if (getContentAsString().contains("javascript:history.go(-1);")) {
                        logger.info("Wrong captcha");
                        if (captchas == 3) throw new PluginImplementationException("Too many captcha attempts");
                        continue;
                    }
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
                }
            }
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private String readCaptchaImage() throws Exception {
        Matcher matcher = getMatcherAgainstContent("src='(.*?/cap.php[^']*)'");
        if (matcher.find()) {
            String s = PlugUtils.replaceEntities(matcher.group(1));
            logger.info("Captcha - image " + s);
            String captcha;
            final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(s);
            final BufferedImage croppedCaptchaImage = captchaImage.getSubimage(1, 1, captchaImage.getWidth() - 2, captchaImage.getHeight() - 2);
            captcha = PlugUtils.recognize(croppedCaptchaImage, "-C A-z-0-9");
            if (captcha != null) {
                logger.info("Captcha - OCR recognized " + captcha);
                //    matcher = PlugUtils.matcher("[A-Z-a-z-0-9]{6}", captcha);
                //   if (!matcher.find()) {
                //       captcha = null;
                //   }
            } else captcha = "";
            captchaImage.flush();//askForCaptcha uvolnuje ten obrazek, takze tady to udelame rucne
            return captcha;
        }
        return "";
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        String content = getContentAsString();
        if (content.contains("The page is temporarily unavailable")) {
            throw new YouHaveToWaitException("The page is temporarily unavailable!", 60 * 2);
        }
        if (content.contains("You must have static IP")) {
            throw new YouHaveToWaitException("You must have static IP! Try again", 60 * 2);
        }
        if (content.contains("file was not found")) {
            throw new URLNotAvailableAnymoreException(String.format("The requested file was not found"));

        }

    }


}