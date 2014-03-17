package cz.vity.freerapid.plugins.services.turbobit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.services.turbobit.captcha.CaptchaReader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;


/**
 * Class which contains main code
 *
 * @author Arthur Gunawan, RickCL, ntoskrnl
 */
class TurboBitFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TurboBitFileRunner.class.getName());
    private final static String LANG_REF = "http://www.turbobit.net/en";
    private final static int CAPTCHA_MAX = 0;
    private int captchaCounter = 1;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(httpMethod)) {
            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(LANG_REF).toGetMethod();
            if (makeRedirectedRequest(httpMethod)) {
                checkProblems();
                checkNameAndSize();
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    /**
     * This method removes all HTML entries, and finds next text after found parameter
     *
     * @param text text
     * @return next text after parameter
     */
    private String findNextTextAfter(final String text) {
        final String contentWithOutHTML[] = getContentAsString().replaceAll("&[^;]{4};", "").split("<[^>]*>");
        boolean found = false;
        for (String x : contentWithOutHTML) {
            if (!found && x.toLowerCase(Locale.ENGLISH).contains(text.toLowerCase(Locale.ENGLISH))) {
                found = true;
                continue;
            }
            if (found && x != null && !x.trim().isEmpty())
                return x.trim();
        }
        return null;
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        httpFile.setFileName(findNextTextAfter("File name:"));
        long size = PlugUtils.getFileSizeFromString(findNextTextAfter("File size:"));
        httpFile.setFileSize(size);

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(httpMethod)) {
            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(LANG_REF).toGetMethod();
            if (makeRedirectedRequest(httpMethod)) {
                checkProblems();
                checkNameAndSize();

                Matcher matcher = PlugUtils.matcher("http://(?:www\\.)?turbobit\\.net/(?:download/free/)?([a-z0-9]+)(?:\\.html?)?", fileURL);
                if (!matcher.find()) {
                    throw new PluginImplementationException("Error parsing download link");
                }
                final String urlCode = matcher.group(1);
                final String freeAction = "http://www.turbobit.net/download/free/" + urlCode + "/";
                httpMethod = getMethodBuilder().setReferer(fileURL).setAction(freeAction).toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }

                matcher = getMatcherAgainstContent("limit: (\\d+),");
                if (matcher.find()) {
                    throw new YouHaveToWaitException("Waiting time between downloads", Integer.parseInt(matcher.group(1)));
                }

                while (getContentAsString().contains("captcha")) {
                    if (!makeRedirectedRequest(stepCaptcha(freeAction))) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                }

                matcher = getMatcherAgainstContent("limit: (\\d+),");
                if (!matcher.find()) {
                    checkProblems();
                    throw new PluginImplementationException("Waiting time not found");
                }
                downloadTask.sleep(Integer.parseInt(matcher.group(1)) + 1);

                final String timeoutAction = "http://www.turbobit.net/download/timeout/" + urlCode + "/";
                httpMethod = getMethodBuilder().setReferer(freeAction).setAction(timeoutAction).toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }

                final String finalURL = "http://turbobit.net/download/redirect/" + PlugUtils.getStringBetween(getContentAsString(), "/download/redirect/", "'");
                logger.info("Final URL: " + finalURL);
                httpMethod = getMethodBuilder().setReferer(timeoutAction).setAction(finalURL).toGetMethod();

                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        Matcher err404Matcher = PlugUtils.matcher("<div class=\"text-404\">(.*?)</div", getContentAsString());
        if (err404Matcher.find()) {
            if (err404Matcher.group(1).contains("\u00d0\u2014\u00d0\u00b0\u00d0\u00bf\u00d1\u20ac\u00d0\u00be\u00d1?\u00d0\u00b5\u00d0\u00bd\u00d0\u00bd\u00d1\u2039\u00d0\u00b9 \u00d0\u00b4\u00d0\u00be\u00d0\u00ba\u00d1?\u00d0\u00bc\u00d0\u00b5\u00d0\u00bd\u00d1\u201a \u00d0\u00bd\u00d0\u00b5 \u00d0\u00bd\u00d0\u00b0\u00d0\u00b9\u00d0\u00b4\u00d0\u00b5\u00d0\u00bd"))
                throw new URLNotAvailableAnymoreException(err404Matcher.group(1));
        }
        if (getContentAsString().contains("\u0424\u0430\u0439\u043B \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D."))
            throw new URLNotAvailableAnymoreException();
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        try {
            Matcher waitMatcher = PlugUtils.matcher("\u00d0\u0178\u00d0\u00be\u00d0\u00bf\u00d1\u20ac\u00d0\u00be\u00d0\u00b1\u00d1?\u00d0\u00b9\u00d1\u201a\u00d0\u00b5\\s+\u00d0\u00bf\u00d0\u00be\u00d0\u00b2\u00d1\u201a\u00d0\u00be\u00d1\u20ac\u00d0\u00b8\u00d1\u201a\u00d1\u0152.*<span id='timeout'>([^>]*)<", getContentAsString());
            if (waitMatcher.find()) {
                throw new YouHaveToWaitException("You have to wait", Integer.valueOf(waitMatcher.group(1)));
            }
            Matcher errMatcher = PlugUtils.matcher("<div[^>]*class='error'[^>]*>([^<]*)<", getContentAsString());
            if (errMatcher.find() && !errMatcher.group(1).isEmpty()) {
                if (errMatcher.group(1).contains("\u00d0?\u00d0\u00b5\u00d0\u00b2\u00d0\u00b5\u00d1\u20ac\u00d0\u00bd\u00d1\u2039\u00d0\u00b9 \u00d0\u00be\u00d1\u201a\u00d0\u00b2\u00d0\u00b5\u00d1\u201a")
                        || errMatcher.group(1).contains("\u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0439 \u043e\u0442\u0432\u0435\u0442!"))
                    throw new CaptchaEntryInputMismatchException();
                throw new PluginImplementationException();
            }
            if (getContentAsString().contains("\u00d0\u00a1\u00d1?\u00d1\u2039\u00d0\u00bb\u00d0\u00ba\u00d0\u00b0 \u00d0\u00bf\u00d1\u20ac\u00d0\u00be\u00d1?\u00d1\u20ac\u00d0\u00be\u00d1\u2021\u00d0\u00b5\u00d0\u00bd\u00d0\u00b0")) // it's unlikely we get this...
                throw new YouHaveToWaitException("Trying again...", 10);
        } catch (NumberFormatException e) {
            throw new PluginImplementationException();
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
                            .setActionFromFormWhereTagContains("recaptcha", true)
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