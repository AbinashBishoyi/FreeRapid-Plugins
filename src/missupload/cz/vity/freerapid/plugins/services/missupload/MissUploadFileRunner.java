package cz.vity.freerapid.plugins.services.missupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MissUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MissUploadFileRunner.class.getName());
    private final int captchaMax = 1;
    private int captchaCounter = 1;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();

        addCookie(new Cookie(".missupload.com", "lang", "english", "/", 86400, false));

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
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "<h1>Download File ", "</h1>");
        PlugUtils.checkFileSize(httpFile, content, "</font> (", ")</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        addCookie(new Cookie(".missupload.com", "lang", "english", "/", 86400, false));

        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setBaseURL(fileURL)
                    .setActionFromFormWhereTagContains("Free Download", true)
                    .removeParameter("method_premium")
                    .toPostMethod();

            if (!makeRedirectedRequest(httpMethod)) throw new ServiceConnectionProblemException();

            /*
            if (getContentAsString().contains("Enter code below")) {
                while (getContentAsString().contains("Enter code below")) {
                    //they have this fugly waiting time between captcha tries which severely limits our possibilities of brute-forcing through it
                    httpMethod = stepCaptcha();
                    downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "<span id=\"countdown\">", "</span>") + 1);

                    if (!tryDownloadAndSaveFile(httpMethod)) {
                        checkProblems();
                        if (getContentAsString().contains("Enter code below")) continue;
                        logger.warning(getContentAsString());
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                }
            } else
                throw new PluginImplementationException("Captcha not found");
            */

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setBaseURL(fileURL)
                    .setActionFromFormByName("F1", true)
                    .removeParameter("method_premium")
                    .toPostMethod();

            downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "<span id=\"countdown\">", "</span>") + 1);

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File Not Found") || content.contains("No such user exist") || content.contains("No such file")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("<Title>Files of ")) {
            throw new PluginImplementationException("Support for link lists not implemented");
        }
    }

    /*
    private HttpMethod stepCaptcha() throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();

        final Matcher matcher = getMatcherAgainstContent("\"(http://www\\.missupload\\.com/captchas/[^\"]+?)\"");
        if (!matcher.find()) throw new PluginImplementationException("Captcha picture not found");
        final String captchaSrc = matcher.group(1);
        logger.info("Captcha URL " + captchaSrc);

        String captcha;
        if (captchaCounter <= captchaMax) {
            final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(captchaSrc);
            captcha = new CaptchaRecognizer().recognize(captchaImage);
            logger.info("OCR attempt " + captchaCounter + " of " + captchaMax + ", recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }

        return getMethodBuilder()
                .setReferer(fileURL)
                .setBaseURL(fileURL)
                .setActionFromFormByName("F1", true)
                .removeParameter("method_premium")
                .removeParameter("code")
                .setParameter("code", captcha)
                .toPostMethod();
    }
    */

}