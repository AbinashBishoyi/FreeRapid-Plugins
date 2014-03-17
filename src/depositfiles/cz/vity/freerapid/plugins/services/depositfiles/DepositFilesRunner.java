package cz.vity.freerapid.plugins.services.depositfiles;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
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
class DepositFilesRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DepositFilesRunner.class.getName());
    private static final String HTTP_DEPOSITFILES = "http://www.depositfiles.com";


    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        fileURL = CheckURL(fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    @Override
    public void run() throws Exception {
        super.run();
        fileURL = CheckURL(fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {

            checkNameAndSize(getContentAsString());
            Matcher matcher;

            if (!getContentAsString().contains("Free downloading mode")) {

                matcher = getMatcherAgainstContent("form action=\"(/.+)\" method");
                if (!matcher.find()) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
                }
                String s = matcher.group(1);

                logger.info("Submit form to - " + s);
                client.setReferer(fileURL);
                final PostMethod postMethod = getPostMethod(HTTP_DEPOSITFILES + s);
                postMethod.addParameter("gateway_result", "1");

                if (!makeRequest(postMethod)) {
                    logger.info(getContentAsString());
                    throw new PluginImplementationException();
                }

            }
            
            //	<img src="http://depositfiles.com/en/get_download_img_code.php?icid=yxcWQT8XPbxGNQxdTxTfEg__"/>
            matcher = getMatcherAgainstContent("src=\"(.*?/get_download_img_code.php[^\"]*)\"" );
            if (matcher.find()) {
                String s = PlugUtils.replaceEntities(matcher.group(1));
                logger.info("Captcha - image " + s);
                String captcha;
                final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(s);
                //logger.info("Read captcha:" + CaptchaReader.read(captchaImage));
                captcha = getCaptchaSupport().askForCaptcha(captchaImage);

                client.setReferer(HTTP_DEPOSITFILES + getMethod.getPath());
                final PostMethod postMethod = getPostMethod(HTTP_DEPOSITFILES + getMethod.getPath());
                PlugUtils.addParameters(postMethod, getContentAsString(), new String[]{"icid"});

                postMethod.addParameter("img_code", captcha);

                if (!makeRequest(postMethod)) {
                    logger.info(getContentAsString());
                    throw new PluginImplementationException();
                }
            }
            
            
            //        <span id="download_waiter_remain">60</span>
            matcher = getMatcherAgainstContent("download_waiter_remain\">([0-9]+)");
            if (!matcher.find()) {
                checkProblems();
                throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
            }
            String t = matcher.group(1);
            int seconds = new Integer(t);
            logger.info("wait - " + t);

            matcher = getMatcherAgainstContent("form action=\"([^\"]*)\" method=\"get\"");
            if (matcher.find()) {
                t = matcher.group(1);
                logger.info("Download URL: " + t);
                downloadTask.sleep(seconds + 1);
                httpFile.setState(DownloadState.GETTING);
                final GetMethod method = getGetMethod(t);
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new IOException("File input stream is empty.");
                }
            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }

        } else
            throw new PluginImplementationException();
    }

    private String CheckURL(String URL) {
        addCookie(new Cookie(".depositfiles.com", "lang_current", "en", "/", 86400, false));

        return URL.replaceFirst("/../files", "/en/files");

    }

    private void checkNameAndSize(String content) throws Exception {
        if (!content.contains("depositfiles")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }

        if (content.contains("file does not exist")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Such file does not exist or it has been removed for infringement of copyrights.</b><br>"));
        }
        Matcher matcher = getMatcherAgainstContent("<b>([0-9.]+&nbsp;.B)</b>");
        if (matcher.find()) {
            logger.info("File size " + matcher.group(1));
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1).replaceAll("&nbsp;", "")));
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }
        matcher = getMatcherAgainstContent("class\\=\"info[^=]*\\=\"([^\"]*)\"");
        if (matcher.find()) {
            final String fn = matcher.group(1);
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
        } else logger.warning("File name was not found" + getContentAsString());
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        String content = getContentAsString();
        if (content.contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>Your IP is already downloading a file from our system.</b><br>You cannot download more than one file in parallel."));
        }
        matcher = Pattern.compile("Please try in\\s*([0-9]+) minute", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(content);
        if (matcher.find()) {
            throw new YouHaveToWaitException("You used up your limit for file downloading!", Integer.parseInt(matcher.group(1)) * 60 + 20);
        }
        matcher = Pattern.compile("Please try in\\s*([0-9]+) hour", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(content);
        if (matcher.find()) {
            throw new YouHaveToWaitException("You used up your limit for file downloading!", Integer.parseInt(matcher.group(1)) * 60 * 60 + 20);
        }
        matcher = Pattern.compile("Please try in\\s*([0-9]+):([0-9]+) hour", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(content);
        if (matcher.find()) {
            throw new YouHaveToWaitException("You used up your limit for file downloading!", Integer.parseInt(matcher.group(1)) * 60 * 60 + Integer.parseInt(matcher.group(2)) * 60 + 20);
        }
        matcher = PlugUtils.matcher("slots[^<]*busy", content);
        if (matcher.find()) {
            throw new YouHaveToWaitException(String.format("<b>All downloading slots for your country are busy</b><br>"), 60 * 2);

        }
        if (content.contains("file does not exist")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Such file does not exist or it has been removed for infringement of copyrights.</b><br>"));

        }

    }


}
