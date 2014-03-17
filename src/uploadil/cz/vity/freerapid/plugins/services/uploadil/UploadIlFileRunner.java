package cz.vity.freerapid.plugins.services.uploadil;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.uploadil.captcha.CaptchaReader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import java.util.regex.Matcher;


/**
 * Class which contains main code
 *
 * @author Frishrash
 */
class UploadIlFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadIlFileRunner.class.getName());
    private final static int CAPTCHA_MAX = 5;
    private int captchaCounter = 1;

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();

        fileURL = fileURL.replaceAll("\\.com/../", ".com/en/");

        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "File name:</b></td>\n<td class=\"data\" dir=\"ltr\">", "</td>");
        PlugUtils.checkFileSize(httpFile, content, "File size:</b></td>\n<td class=\"data\">", "</td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        fileURL = fileURL.replaceAll("\\.com/../", ".com/en/");
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            // Do while captcha isn't broken. After CAPTCHA_MAX failed 
            // recognition attempts it would ask the user for manual input
            while (getContentAsString().contains("captcha")) {
                if (!makeRequest(stepCaptcha())) {
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            }

            Matcher matcher = getMatcherAgainstContent("id=\"downloadbtn\"[^<>]+?onclick='document.location=\"(.+?)\"'");
            if (!matcher.find()) throw new PluginImplementationException("Download link not found");
            final String downloadURL = matcher.group(1);

            if (getContentAsString().contains("Please wait")) {
                downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "var timeout='", "'") + 1);
            }

            //we don't want "upload-il.com" in the filename
            client.getHTTPClient().getParams().setParameter("dontUseHeaderFilename", true);

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(getGetMethod(downloadURL))) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new ServiceConnectionProblemException("Error starting download");
            }

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file is not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        } else if (contentAsString.contains("check back later")) {
            throw new YouHaveToWaitException("Due to high demand at the moment, this file is available only to registered users", 60 * 60);
        }
    }

    private HttpMethod stepCaptcha() throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaURL = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getEscapedURI();
        logger.info("Captcha URL " + captchaURL);

        final String captcha;
        if (captchaCounter <= CAPTCHA_MAX) {
            final BufferedImage captchaImage = captchaSupport.getCaptchaImage(captchaURL);
            final BufferedImage preparedImage = CaptchaReader.getPreparedImage(captchaImage);

            //JOptionPane.showConfirmDialog(null, new ImageIcon(preparedImage));

            captcha = CaptchaReader.recognize(preparedImage);
            logger.info("Attempt " + captchaCounter + " of " + CAPTCHA_MAX + ", OCR recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaURL);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }

        return getMethodBuilder()
                .setReferer(fileURL)
                .setBaseURL(fileURL)
                .setActionFromFormByName("myform", true)
                .setParameter("captchacode", captcha)
                .toPostMethod();
    }

}