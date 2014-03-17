package cz.vity.freerapid.plugins.services.filesonic;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author JPEXS, ntoskrnl
 */
class FileSonicFileRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(FileSonicFileRunner.class.getName());

    private void ensureENLanguage() {
        final Matcher m = PlugUtils.matcher("http://(?:www\\.)?filesonic.com/(?:[^/]*/)?(file/.*)", fileURL);
        if (m.matches()) {
            fileURL = "http://www.filesonic.com/en/" + m.group(1);
        }
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        ensureENLanguage();
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
        PlugUtils.checkName(httpFile, getContentAsString(), "<span>Filename: </span> <strong>", "</strong>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<span class=\"size\">", "</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        ensureENLanguage();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            fileURL = fileURL.replaceFirst("/en/", "/");
            final String startUrl = fileURL + "?start=1";
            method = getMethodBuilder().setReferer(fileURL).setAction(startUrl).toPostMethod();
            while (true) {
                method.addRequestHeader("X-Requested-With", "XMLHttpRequest");
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                final String content = getContentAsString();
                if (content.contains("Recaptcha")) {
                    final String reCaptchaKey = PlugUtils.getStringBetween(content, "Recaptcha.create(\"", "\"");
                    final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
                    final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
                    if (captcha == null) {
                        throw new CaptchaEntryInputMismatchException();
                    }
                    r.setRecognized(captcha);
                    method = r.modifyResponseMethod(getMethodBuilder().setReferer(fileURL).setAction(startUrl)).toPostMethod();
                } else if (content.contains("var countDownDelay =")) {
                    final String tm = PlugUtils.getParameter("tm", content);
                    final String tm_hash = PlugUtils.getParameter("tm_hash", content);
                    method = getMethodBuilder().setReferer(fileURL).setAction(startUrl).setParameter("tm", tm).setParameter("tm_hash", tm_hash).toPostMethod();
                    final int waitTime = PlugUtils.getWaitTimeBetween(content, "var countDownDelay =", ";", TimeUnit.SECONDS);
                    downloadTask.sleep(waitTime + 1);
                } else if (content.contains("Start download now")) {
                    method = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Start download now").toGetMethod();
                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                    break;
                } else {
                    checkProblems();
                    throw new PluginImplementationException();
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("for parallel downloads")) {
            throw new YouHaveToWaitException("Already processing a download", 30);
        }
        if (content.contains("File not found") || content.contains("This file was deleted")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}
