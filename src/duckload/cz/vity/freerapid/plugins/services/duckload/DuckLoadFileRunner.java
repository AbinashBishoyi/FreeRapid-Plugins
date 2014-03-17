package cz.vity.freerapid.plugins.services.duckload;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.duckload.captcha.CaptchaRecognizer;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class DuckLoadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DuckLoadFileRunner.class.getName());
    private final static Random random = new Random();
    private static CaptchaRecognizer captchaRecognizer;
    private final static int CAPTCHA_MAX = 5;
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
        final Matcher matcher = getMatcherAgainstContent("Datei \"(.+?)\" \\((.+?)\\)");
        if (!matcher.find()) throw new PluginImplementationException("File name/size not found");
        httpFile.setFileName(matcher.group(1));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            while (true) {
                downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "var t_time=", ";") + 1);

                if (!tryDownloadAndSaveFile(stepCaptcha())) {
                    checkProblems();
                    final String content = getContentAsString();
                    if (content.contains("Code ist Falsch") || content.contains("Bitte diesen Code")) {
                        if (!makeRedirectedRequest(method)) throw new ServiceConnectionProblemException();
                        continue;
                    }
                    throw new ServiceConnectionProblemException("Error starting download");
                } else break;
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Datei wurde nicht gefunden") || content.contains("<h1>404 - Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private HttpMethod stepCaptcha() throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("Captcha").getEscapedURI();
        logger.info("Captcha URL " + captchaSrc);

        final String captcha;
        if (captchaCounter <= CAPTCHA_MAX) {
            if (captchaRecognizer == null) {//initiate captchaRecognizer only once
                captchaRecognizer = new CaptchaRecognizer();
            }
            captcha = captchaRecognizer.recognize(captchaSupport.getCaptchaImage(captchaSrc));
            if (captcha == null) {
                logger.info("Could not separate captcha letters, attempt " + captchaCounter + " of " + CAPTCHA_MAX);
            }
            logger.info("OCR recognized " + captcha + ", attempt " + captchaCounter + " of " + CAPTCHA_MAX);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }

        return getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("Captcha", true)
                .removeParameter("_____download")
                .setParameter("cap", captcha)
                .setParameter("_____download.x", String.valueOf(random.nextInt(100)))
                .setParameter("_____download.y", String.valueOf(random.nextInt(100)))
                .toPostMethod();
    }

    @Override
    protected String getBaseURL() {
        return "http://www.duckload.com";
    }

}