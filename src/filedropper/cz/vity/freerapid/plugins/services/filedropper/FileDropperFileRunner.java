package cz.vity.freerapid.plugins.services.filedropper;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class FileDropperFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileDropperFileRunner.class.getName());
    private final int captchaMax = 1;
    private int captchaCounter = 1;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod method = getGetMethod(fileURL);
        makeRedirectedRequest(method);//always returns 404 for some reason
        checkProblems();
        if (!isImage()) checkNameAndSize();
    }

    private boolean isImage() {
        return getContentAsString().contains("Image Hosted at");
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "Filename: ", " <br>");
        PlugUtils.checkFileSize(httpFile, content, "Size: ", ", ");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        makeRedirectedRequest(method);
        checkProblems();

        if (isImage()) {
            logger.info("Link type: image");

            final MethodBuilder methodBuilder = getMethodBuilder()
                    .setReferer(fileURL)
                    .setBaseURL("http://www.filedropper.com/")
                    .setActionFromImgSrcWhereTagContains("");

            final String secondURL = methodBuilder.getAction();
            String name;
            try {
                name = fileURL.substring(fileURL.lastIndexOf("/") + 1)//file name
                        + secondURL.substring(secondURL.lastIndexOf("."));//file extension
            } catch (StringIndexOutOfBoundsException e) {
                logger.warning("Error getting name: " + e.toString());
                name = "unnamed";
            }
            httpFile.setFileName(name);

            if (!tryDownloadAndSaveFile(methodBuilder.toGetMethod())) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkNameAndSize();

            if (getContentAsString().contains("enter the characters")) {
                while (getContentAsString().contains("enter the characters")) {
                    if (!tryDownloadAndSaveFile(stepCaptcha())) {
                        checkProblems();
                        if (getContentAsString().contains("enter the characters")) continue;
                        logger.warning(getContentAsString());
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                }
            } else {
                throw new PluginImplementationException("Captcha not found");
            }
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("<title>Free File Hosting - ")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private HttpMethod stepCaptcha() throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = "http://www.filedropper.com/" + getMethodBuilder().setActionFromImgSrcWhereTagContains("securimage").getAction();
        logger.info("Captcha URL " + captchaSrc);

        String captcha;
        if (captchaCounter <= captchaMax) {
            //the captchas are really tough, but might just leave this in anyway...
            captcha = PlugUtils.recognize(captchaSupport.getCaptchaImage(captchaSrc), "-d -1 -C A-Z-0-9");
            logger.info("OCR attempt " + captchaCounter + " of " + captchaMax + ", recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }

        return getMethodBuilder()
                .setReferer(fileURL)
                .setBaseURL("http://www.filedropper.com/")
                .setActionFromFormByName("myform", true)
                .setParameter("code", captcha)
                .toPostMethod();
    }

}