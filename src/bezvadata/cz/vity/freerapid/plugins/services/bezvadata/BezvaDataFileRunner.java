package cz.vity.freerapid.plugins.services.bezvadata;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.ByteArrayInputStream;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class BezvaDataFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BezvaDataFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<b>Soubor:", "</b>");
        PlugUtils.checkFileSize(httpFile, content, "Velikost:</strong>", "</li>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("Stáhnout soubor")
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            while (getContentAsString().contains("frm-stahnoutFreeForm")) {
                stepCaptcha();
            }
            downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "countdown\">00:", "</"));

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("Stáhnout soubor")
                    .toGetMethod();
            setFileStreamContentTypes("text/plain");
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void stepCaptcha() throws Exception {
        final String captchaImgBase64Str = PlugUtils.getStringBetween(getContentAsString(), "data:image/png;base64,", "\"");
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captcha = captchaSupport.askForCaptcha(captchaSupport.loadCaptcha(new ByteArrayInputStream(Base64.decodeBase64(captchaImgBase64Str))));
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        final HttpMethod method = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("frm-stahnoutFreeForm", true)
                .setParameter("stahnoutSoubor", "Stáhnout")
                .setParameter("captcha", captcha)
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Soubor nenalezen") || contentAsString.contains("nebyla nalezena")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("plně vytížené všechny sloty pro stahování Zdarma")) {
            throw new YouHaveToWaitException("All free slots are full", 2 * 60);
        }
    }

}