package cz.vity.freerapid.plugins.services.easyshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;

/**
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
 */
class EasyShareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(EasyShareRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".easy-share.com", "language", "en", "/", null, false));
        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        PlugUtils.checkName(httpFile, getContentAsString(), "requesting:</span>", "<span");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<span class=\"txtgray\">(", ")</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(".easy-share.com", "language", "en", "/", null, false));
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
            do {
                downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "var wf = ", ";") + 1);
                while (!getContentAsString().contains("Recaptcha.create(\"")) {
                    final int w = PlugUtils.getNumberBetween(getContentAsString(), "w='", "'");
                    if (w != 1200 && w != 1800) {//these even numbers are wrong, refresh the time below
                        if (w > 120) {
                            throw new YouHaveToWaitException("Waiting time between downloads", w);
                        } else {
                            downloadTask.sleep(w);
                        }
                    }
                    final String u = PlugUtils.getStringBetween(getContentAsString(), "u='", "'");
                    final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(u).toGetMethod();
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

    private void checkProblems() throws Exception {
        final String content = getContentAsString();
        if (content.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("Requested file is deleted")) {
            throw new URLNotAvailableAnymoreException("Requested file is deleted");
        }
        if (content.contains("Page not found")) {
            throw new InvalidURLOrServiceProblemException("Page not found");
        }
        if (content.contains("You have downloaded ")) {
            throw new YouHaveToWaitException("You have downloaded to much during last hour. You have to wait", 20 * 60);
        }
        if (content.contains("This file will be available soon")) {
            throw new ServiceConnectionProblemException("This file will be available soon");
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

    @Override
    protected String getBaseURL() {
        return "http://www.easy-share.com";
    }

}