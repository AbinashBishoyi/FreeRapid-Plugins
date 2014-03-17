package cz.vity.freerapid.plugins.services.webo999;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
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
 * @author birchie
 */
class Webo999FileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Webo999FileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            if (getContentAsString().contains("human_check.php")) {
                throw new YouHaveToWaitException("Trying to skip human check", 1);
            }
            checkProblems();
            checkNameAndSize();//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<Strong>", "</Strong></a>");
        // no file size available
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            while (getContentAsString().contains("human_check.php")) {
                doCaptcha();
            }
            checkNameAndSize();//extract file name and size from the page
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("<Strong>" + httpFile.getFileName()).toGetMethod();
            setClientParameter(DownloadClientConsts.NO_CONTENT_LENGTH_AVAILABLE, true);
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws Exception {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("onmouseover=\"show_sn")) {
            if (PlugUtils.getStringBetween(contentAsString, "<a href", "onmouseover=\"show_sn").contains("search.webo999")) {
                throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
            }
        }
    }

    private void doCaptcha() throws Exception {
        final String captchaImg = "http://webo999.com/human_check.php";
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaTxt = captchaSupport.getCaptcha(captchaImg);
        if (captchaTxt == null) throw new CaptchaEntryInputMismatchException("No Input");
        final MethodBuilder mb = getMethodBuilder()
                .setActionFromFormWhereActionContains("download.php", true)
                .setParameter("human_check", captchaTxt);
        if (!makeRedirectedRequest(mb.toPostMethod())) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

}