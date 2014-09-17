package cz.vity.freerapid.plugins.services.turbobit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.services.turbobit.captcha.CaptchaReader;
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
 * @author Arthur Gunawan, RickCL, ntoskrnl, tong2shot, Abinash Bishoyi, birchie
 */
public class TurboBitFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TurboBitFileRunner.class.getName());

    private final static int CAPTCHA_MAX = 0;
    private int captchaCounter = 1;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        fileURL = checkFileURL(fileURL);
        final HttpMethod method = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(method)) {
            checkFileProblems();
            checkNameAndSize();
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String checkFileURL(final String fileURL) throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("^http://(?:www\\.|new\\.)?(turbobit\\.net|hitfile\\.net|files\\.uz-translations\\.uz)/(?:download/free/)?(\\w+)", fileURL.replaceFirst("turo-bit.net/", "turbobit.net/"));
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing download link");
        }
        addCookie(new Cookie("." + matcher.group(1), "user_lang", "en", "/", 86400, false));
        return "http://" + matcher.group(1) + "/download/free/" + matcher.group(2);
    }

    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<span class.*?>(.+?)</span>\\s*\\((.+?)\\)");
        if (matcher.find()) {
            httpFile.setFileName(matcher.group(1));
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        } else {
            PlugUtils.checkName(httpFile, getContentAsString(), "file-title\">", "<");
            PlugUtils.checkFileSize(httpFile, getContentAsString(), "file-size\">", "</");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        fileURL = checkFileURL(fileURL);

        HttpMethod method = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            Matcher matcher = getMatcherAgainstContent("limit\\s*:\\s*(\\d+)");
            if (matcher.find()) {
                throw new YouHaveToWaitException("Download limit reached", Integer.parseInt(matcher.group(1)));
            }

            while (getContentAsString().contains("/captcha/")) {
                if (!makeRedirectedRequest(stepCaptcha(method.getURI().toString()))) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            }

            matcher = getMatcherAgainstContent("Timeout/(.+?)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("File ID not found");
            }

            method = getMethodBuilder()
                    .setReferer(method.getURI().toString())
                    .setAction("/download/getLinkTimeout/" + matcher.group(1))
                    .setBaseURL(method.getURI().toString().split("/download/")[0])
                    .toGetMethod();
            method.addRequestHeader("X-Requested-With", "XMLHttpRequest");

            downloadTask.sleep(61);

            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            if (getContentAsString().contains("Error: 2965")) {
                throw new PluginImplementationException("Plugin is broken");
            }

            method = getMethodBuilder()
                    .setReferer(method.getURI().toString())
                    .setActionFromAHrefWhereATagContains("Download")
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

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("File was not found")
                || getContentAsString().contains("Probably it was deleted"))
            throw new URLNotAvailableAnymoreException("File not found");
        if (getContentAsString().contains("Our service is currently unavailable in your country"))
            throw new NotRecoverableDownloadException("Service is unavailable in your country");
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("The file is not available now because of technical problems")) {
            throw new ServiceConnectionProblemException("The file is not available now because of technical problems");
        }
        if (getContentAsString().contains("From your IP range the limit of connections is reached")) {
            final int waitTime = PlugUtils.getNumberBetween(getContentAsString(), " id='timeout'>", "</");
            throw new YouHaveToWaitException("Download limit reached from your IP range", waitTime);
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        checkDownloadProblems();
    }

    private HttpMethod stepCaptcha(final String action) throws Exception {
        if (getContentAsString().contains("recaptcha")) {
            logger.info("Handling ReCaptcha");

            final Matcher m = getMatcherAgainstContent("api.recaptcha.net/noscript\\?k=([^\"]+)\"");
            if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");
            final String reCaptchaKey = m.group(1);

            final String content = getContentAsString();
            final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
            final CaptchaSupport captchaSupport = getCaptchaSupport();

            final String captchaURL = r.getImageURL();
            logger.info("Captcha URL " + captchaURL);

            final String captcha = captchaSupport.getCaptcha(captchaURL);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            r.setRecognized(captcha);

            return r.modifyResponseMethod(
                    getMethodBuilder(content)
                            .setReferer(action)
                            .setActionFromFormByIndex(3, true)
                            .setAction(action)
            ).toPostMethod();
        } else {
            logger.info("Handling regular captcha");

            final CaptchaSupport captchaSupport = getCaptchaSupport();
            final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getEscapedURI();
            logger.info("Captcha URL " + captchaSrc);

            final String captcha;
            if (captchaCounter <= CAPTCHA_MAX) {
                captcha = CaptchaReader.recognize(captchaSupport.getCaptchaImage(captchaSrc));
                if (captcha == null) {
                    logger.info("Could not separate captcha letters, attempt " + captchaCounter + " of " + CAPTCHA_MAX);
                }
                logger.info("OCR recognized " + captcha + ", attempt " + captchaCounter + " of " + CAPTCHA_MAX);
                captchaCounter++;
            } else {
                captcha = captchaSupport.getCaptcha(captchaSrc);
                if (captcha == null) throw new CaptchaEntryInputMismatchException();
                logger.info("Manual captcha " + captcha);
            }

            return getMethodBuilder()
                    .setReferer(action)
                    .setActionFromFormWhereTagContains("captcha", true)
                    .setAction(action)
                    .setParameter("captcha_response", captcha)
                    .toPostMethod();
        }
    }

}