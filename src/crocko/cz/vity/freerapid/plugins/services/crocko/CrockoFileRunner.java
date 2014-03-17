package cz.vity.freerapid.plugins.services.crocko;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class CrockoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CrockoFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".crocko.com", "language", "en", "/", null, false));
        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<span class=\"fz24\">\\s*Download:\\s*<strong>(.+?)</strong>\\s*</span>\\s*<span class=\"tip1\">\\s*<span class=\"inner\">(.+?)</span>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name/size not found");
        }
        httpFile.setFileName(matcher.group(1));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".crocko.com", "language", "en", "/", null, false));
        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            fileURL = getMethod.getURI().toString();
            checkProblems();
            checkNameAndSize();
            do {
                while (!getContentAsString().contains("Recaptcha.create(\"")) {
                    final int wait = PlugUtils.getNumberBetween(getContentAsString(), "w='", "'");
                    final HttpMethod method = getMethodBuilder().setReferer(fileURL).setActionFromTextBetween("u='", "'").toGetMethod();
                    if (wait > 120) {
                        throw new YouHaveToWaitException("Waiting time between downloads", wait);
                    } else {
                        downloadTask.sleep(wait);
                    }
                    method.addRequestHeader("X-Requested-With", "XMLHttpRequest");
                    if (!makeRedirectedRequest(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                }
            } while (!tryDownloadAndSaveFile(stepCaptcha()));
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("the page you're looking for") || content.contains("Requested file is deleted")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("The requested file is temporarily unavailable")) {
            throw new ServiceConnectionProblemException("The requested file is temporarily unavailable");
        }
        if (content.contains("There is another download in progress from your IP")) {
            throw new ServiceConnectionProblemException("There is another download in progress from your IP");
        }
    }

    private HttpMethod stepCaptcha() throws Exception {
        final String content = getContentAsString();
        final String reCaptchaKey = PlugUtils.getStringBetween(content, "Recaptcha.create(\"", "\"");
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final CaptchaSupport captchaSupport = getCaptchaSupport();

        final String captchaURL = r.getImageURL();
        logger.info("Captcha URL " + captchaURL);

        final String captcha = captchaSupport.getCaptcha(captchaURL);
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        r.setRecognized(captcha);

        return r.modifyResponseMethod(getMethodBuilder(content).setReferer(fileURL).setActionFromFormWhereTagContains("recaptcha", true)).toPostMethod();
    }

}