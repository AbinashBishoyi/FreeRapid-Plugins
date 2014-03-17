package cz.vity.freerapid.plugins.services.extabit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Thumb, ntoskrnl, RickCL
 */
class ExtabitFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ExtabitFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<title>", "download Extabit.com - file hosting</title>");
        try {
            PlugUtils.checkFileSize(httpFile, getContentAsString(), "Size:", "</div>");
        } catch (Exception e) {
            PlugUtils.checkFileSize(httpFile, getContentAsString(), "<td class=\"col-fileinfo\">", "</td>");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkDownloadProblems();
            checkNameAndSize();
            fileURL = method.getURI().toString();

            Matcher matcher;
            final String contentAsString = getContentAsString(); //get page content that contains captcha
            final long startTime = System.currentTimeMillis();
            do {
                method = stepCaptcha(contentAsString);
                method.addRequestHeader("X-Requested-With", "XMLHttpRequest");
                final long toWait = startTime + 31000 - System.currentTimeMillis();
                if (toWait > 0) {
                    downloadTask.sleep((int) Math.ceil(toWait / 1000d));
                }
                if (!makeRedirectedRequest(method)) {
                    checkDownloadProblems();
                    throw new ServiceConnectionProblemException();
                }
            } while (!(matcher = getMatcherAgainstContent("\"href\"\\s*:\\s*\"(.+?)\"")).find());

            //logger.info(getContentAsString());

            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(matcher.group(1).replace("\\/", "/"))
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkDownloadProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("File is temporary unavailable")) {
            throw new ServiceConnectionProblemException("File is temporarily unavailable");
        }
        if (getContentAsString().contains("The file that you're trying to download is larger than")
                || getContentAsString().contains("Only premium users can download this file")) {
            throw new NotRecoverableDownloadException("Only premium users can download this file");
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        checkProblems();
        if (getContentAsString().contains("Next free download from your ip will be available in")) {
            final int waitTime = PlugUtils.getWaitTimeBetween(getContentAsString(), "Next free download from your ip will be available in <b>", " minutes</b>", TimeUnit.MINUTES);
            throw new YouHaveToWaitException("Next free download from your ip will be available in", waitTime);
        }
    }

    private HttpMethod stepCaptcha(String content) throws ErrorDuringDownloadingException {
        final String captchaUrl = getMethodBuilder(content).setAction("/capture.gif?" + new Random().nextInt()).getEscapedURI();
        final String captcha = getCaptchaSupport().getCaptcha(captchaUrl);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        return getMethodBuilder(content)
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("cmn_form", false)
                .setParameter("link", "1")
                .setParameter("capture", captcha)
                .toGetMethod();
    }

    @Override
    protected String getBaseURL() {
        return "http://extabit.com";
    }

}