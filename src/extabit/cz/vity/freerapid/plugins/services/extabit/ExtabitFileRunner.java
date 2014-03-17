package cz.vity.freerapid.plugins.services.extabit;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.Random;
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
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<th>Size:</th>\n<td class=\"col-fileinfo\">", "</td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            Matcher matcher;
            final long startTime = System.currentTimeMillis();
            do {
                method = stepCaptcha();
                final long toWait = startTime + 31000 - System.currentTimeMillis();
                if (toWait > 0) {
                    downloadTask.sleep((int) Math.ceil(toWait / 1000d));
                }
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            } while (!(matcher = getMatcherAgainstContent("\"href\"\\s*:\\s*\"(.+?)\"")).find());

            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(matcher.group(1).replace("\\/", "/"))
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
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
    }

    private HttpMethod stepCaptcha() throws ErrorDuringDownloadingException {
        final String captchaUrl = getMethodBuilder().setAction("/capture.gif?" + new Random().nextInt()).getEscapedURI();
        final String captcha = getCaptchaSupport().getCaptcha(captchaUrl);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        return getMethodBuilder()
                .setReferer(fileURL)
                .setAction(fileURL)
                .setParameter("link", "1")
                .setParameter("capture", captcha)
                .toGetMethod();
    }

}