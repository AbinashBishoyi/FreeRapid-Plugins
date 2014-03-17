package cz.vity.freerapid.plugins.services.ulozisko;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Kajda
 * @since 0.82
 */
class UloziskoFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(UloziskoFileRunner.class.getName());
    private static final String SERVICE_WEB = "http://www.ulozisko.sk";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setPageEncoding("Windows-1250");
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkSeriousProblems();
            checkNameAndSize();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        setPageEncoding("Windows-1250");
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkNameAndSize();
            httpMethod = stepCaptcha();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkAllProblems();
                checkLocation(httpMethod);
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty");
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("Zadaný súbor neexistuje")) {
            throw new URLNotAvailableAnymoreException("Zadaný súbor neexistuje");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("Neopísali ste správny overovací reťazec")) {
            throw new YouHaveToWaitException("Neopísali ste správny overovací reťazec", 4);
        }

        if (contentAsString.contains("Prekročili ste download-limit pre free používateľov")) {
            throw new YouHaveToWaitException("Prekročili ste download-limit pre free používateľov", PlugUtils.getWaitTimeBetween(contentAsString, "cas = ", ";", TimeUnit.SECONDS));
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "\">", "</div><br /><br /><br /><b>Detaily");
        PlugUtils.checkFileSize(httpFile, contentAsString, "Veľkosť súboru: <strong>", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private HttpMethod stepCaptcha() throws ErrorDuringDownloadingException {
        final String captchaSrc = SERVICE_WEB + PlugUtils.getStringBetween(getContentAsString(), "<br /><img src=\"", "\"");
        logger.info("Captcha URL " + captchaSrc);
        final String captcha = getCaptchaSupport().getCaptcha(captchaSrc);

        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            return getMethodBuilder().setReferer(fileURL).setActionFromFormByName("formular", true).setBaseURL(SERVICE_WEB).setParameter("antispam", captcha).toHttpMethod();
        }
    }

    private void checkLocation(HttpMethod httpMethod) throws ErrorDuringDownloadingException {
        if (httpMethod.getResponseHeader("Location").getValue().contains("error2")) {
            throw new YouHaveToWaitException("You can not download now", 4);
        }
    }
}