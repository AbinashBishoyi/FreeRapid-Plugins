package cz.vity.freerapid.plugins.services.filesavr;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
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
class FileSavrFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileSavrFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        //for some reason FileSavr always returns HTTP status 404
        makeRedirectedRequest(getMethod);//) {
        checkProblems();
        checkNameAndSize();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String fn = fileURL.substring(fileURL.lastIndexOf('/') + 1);
        this.httpFile.setFileName(fn);
        this.httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        //for some reason FileSavr always returns HTTP status 404
        makeRedirectedRequest(method);
        checkProblems();
        checkNameAndSize();

        while (getContentAsString().contains("Please enter the characters")) {
            tryDownloadAndSaveFile(stepCaptcha());
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (!getContentAsString().contains("Please enter the characters")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private HttpMethod stepCaptcha() throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = "http://www.filesavr.com/" + PlugUtils.getStringBetween(getContentAsString(), "<img id=\"img\" src=\"", "\">");
        logger.info("Captcha URL " + captchaSrc);
        final String captcha = captchaSupport.getCaptcha(captchaSrc);

        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            return getMethodBuilder().setReferer(fileURL).setBaseURL("http://www.filesavr.com/").setActionFromFormByName("myform", true).setAndEncodeParameter("code", captcha).toPostMethod();
        }
    }

}