package cz.vity.freerapid.plugins.services.megashare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl, Tommy
 */

/**
 * Request and response data by Tommy[ywx217@gmail.com]
 * 1. First page of http://www.megashare.com/4433199 click download link, then should wait for 10 sec.
 * Request URL:http://www.megashare.com/4433199
 * Request Method:POST
 * Referer:http://www.megashare.com/4433199
 * Form data:
 * 4433199prZVal:2596608
 * f£0Dl75314876.x:34
 * f£0Dl75314876.y:33
 * f£0Dl75314876:FREE
 * Cookie:(I'm from China, so geo code is CN, I think there's no need to handle the cookies)
 * PHPSESSID=c9fbbb5d62rcuo039ade5qq9a6
 * geoCode=CN
 * <p/>
 * 2. After 10 seconds of waiting, it's the download page with captcha image.
 * Request URL:http://www.megashare.com/4433199
 * Request Method:POST
 * Referer:http://www.megashare.com/4433199
 * Form data:
 * wComp:1
 * 4433199prZVal:5780228
 * id:4433199
 * time_diff:1344232440
 * req_auth:n
 * Cookies are the same with the above.
 * Captcha image URL:
 * security.php?i=44331991344232451&sid=4433199
 * Notes:
 * The parameter "i" is the combination of id and the time_diff value of this page, the time_diff
 * value(1344232451) is different from the request param time_diff(1344232440) of this page; and
 * the parameter "sid" is obviously is same with id.
 * <p/>
 * 3. Enter the right captcha, start download..
 * Request URL:http://www.megashare.com/dnd/4433199/bdab773eb22f5212b0384b0c6e7b65b6/r221.zip
 * Request Method:GET
 * Referer:http://www.megashare.com/4433199
 * Form data: no form data.
 * Cookie:
 * PHPSESSID=c9fbbb5d62rcuo039ade5qq9a6
 * geoCode=CN
 * __atuvc=1%7C32  (domain=www.megashare.com; path=/)
 */
class MegaShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MegaShareFileRunner.class.getName());
    private final static int CAPTCHA_MAX = 3;
    private int captchaCounter = 1;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (isRedirect()) return;
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private boolean isRedirect() {
        return fileURL.contains("r=");
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        // They provide absolutely no info about the file.
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);

        if (makeRedirectedRequest(method)) {
            if (isRedirect()) {
                final String location = PlugUtils.getStringBetween(getContentAsString(), "location.replace('", "');");
                if (location.equalsIgnoreCase("http://www.megashare.com")) {
                    throw new URLNotAvailableAnymoreException("Redirect target not found");
                }
                httpFile.setNewURL(new URL(location));
                httpFile.setPluginID("");
                httpFile.setState(DownloadState.QUEUED);
                return;
            }

            checkProblems();
            checkNameAndSize();

            // First stage process.
            HttpMethod httpMethod = processFirstStage();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "var cTmr =", ";") + 1);

            httpMethod = processSecondStage();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            // Now it's the page contains captcha image.
            do {
                if (captchaCounter > CAPTCHA_MAX) throw new YouHaveToWaitException("Retry request", 3);
                httpMethod = stepCaptcha();
                makeRequest(httpMethod);
                checkProblems();
                final Header h = httpMethod.getResponseHeader("Location");
                if (h != null && h.getValue() != null && !h.getValue().isEmpty()) {
                    httpMethod = getMethodBuilder().setReferer(fileURL).setAction(h.getValue()).toGetMethod();

                    if (tryDownloadAndSaveFile(httpMethod)) {
                        break;
                    } else {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                }
            } while (getContentAsString().contains("name=\"captcha_code\""));

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        /* false positive
        if (content.contains("have a look here")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
        */
        if (content.contains("This file has been DELETED")) {
            final Matcher matcher = getMatcherAgainstContent("<div> (Reason:.+?) </div>");
            if (matcher.find()) {
                throw new URLNotAvailableAnymoreException("This file has been deleted. " + matcher.group(1));
            }
            throw new URLNotAvailableAnymoreException("This file has been deleted");
        }
    }

    private HttpMethod stepCaptcha() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("<img id=\".+?\" src=\"(security\\.php.+?)\"");
        if (!matcher.find()) throw new PluginImplementationException("Captcha image not found");
        final String captchaSrc = getBaseURL() + matcher.group(1);
        logger.info("Captcha URL " + captchaSrc);

        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captcha;
        if (captchaCounter <= (CAPTCHA_MAX - 1)) {
            captcha = new CaptchaRecognizer().recognize(captchaSupport.getCaptchaImage(captchaSrc));
            logger.info("OCR attempt " + captchaCounter + " of " + CAPTCHA_MAX + ", recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }
        final MethodBuilder methodBuilder = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormByName("downloader", true)
                .setAction(fileURL)
                .setParameter("captcha_code", captcha);
        if (getContentAsString().contains("is_captcha=1;")) {
            methodBuilder.removeParameter("yesss").removeParameter("yesss.x").removeParameter("yesss.y");
        } else {
            methodBuilder.setAction(getBaseURL() + "download.php").setParameter("yesss", "Loading Download...").removeParameter("user_val");
        }
        return methodBuilder.toPostMethod();
    }

    @Override
    protected String getBaseURL() {
        return "http://www.megashare.com/";
    }

    private HttpMethod processFirstStage() throws URLNotAvailableAnymoreException, PluginImplementationException {
        if (!getContentAsString().contains("please-scroll.png"))
            throw new URLNotAvailableAnymoreException("Not downloading page");
        return getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("please-scroll.png", true)
                .setAction(fileURL)
                .removeParameter("PremDz")
                .toPostMethod();
    }

    private HttpMethod processSecondStage() throws URLNotAvailableAnymoreException, PluginImplementationException {
        return getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormByName("downloader", true)
                .setAction(fileURL)
                .setParameter("wComp", "1")
                .toPostMethod();
    }
}