package cz.vity.freerapid.plugins.services.missupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MissUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MissUploadFileRunner.class.getName());
    private final static int captchaMax = 5;
    private int captchaCounter = 1;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        //PlugUtils.checkName(httpFile, content, "FileNameLEFT", "FileNameRIGHT");//TODO
        //PlugUtils.checkFileSize(httpFile, content, "FillSizeLEFT", "FileSizeRIGHT");//TODO
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();//check problems
            checkNameAndSize();//extract file name and size from the page

            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setBaseURL(fileURL)
                    .setActionFromFormWhereTagContains("Free Download", true)
                    .removeParameter("method_premium")
                    .toPostMethod();

            makeRedirectedRequest(httpMethod);

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setBaseURL(fileURL)
                    .setActionFromFormByName("F1", true)
                    .removeParameter("method_premium")
                    .removeParameter("code")
                    .setParameter("code", stepCaptcha())
                    .toPostMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException();
            }

            logger.info(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String stepCaptcha() throws ErrorDuringDownloadingException {
        /*final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("kcapcha").getAction();
        logger.info("Captcha URL " + captchaSrc);

        String captcha;
        if (captchaCounter <= captchaMax) {
            captcha = PlugUtils.recognize(captchaSupport.getCaptchaImage(captchaSrc), "-d -1 -C 0-9");
            logger.info("OCR attempt " + captchaCounter + " of " + captchaMax + ", recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }*/

        logger.info(getContentAsString());
        final Matcher matcher = getMatcherAgainstContent("&#(\\d\\d);");
        String captcha = "";

        //for (int i = 1; i < 5; i++) {
        captcha += matcher.group(1);
        //}

        logger.info(captcha);
        return captcha;
        //return Integer.toString(captcha);
        //return getMethodBuilder().setReferer(fileURL).setActionFromFormWhereTagContains("kcapcha", true).setParameter("kcapcha", captcha).toPostMethod();
    }

}