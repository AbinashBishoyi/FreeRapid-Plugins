package cz.vity.freerapid.plugins.services.letitbit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.letitbit.captcha.CaptchaReader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
 */
class LetitbitRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LetitbitRunner.class.getName());
    private int captchatry = 0;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".letitbit.net", "lang", "en", "/", 86400, false));
        setPageEncoding("Windows-1251");
        final HttpMethod httpMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "File:: <span>", "</span>");
        PlugUtils.checkFileSize(httpFile, contentAsString, "[<span>", "</span>]");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".letitbit.net", "lang", "en", "/", 86400, false));
        setPageEncoding("Windows-1251");
        client.getHTTPClient().getParams().setBooleanParameter("dontUseHeaderFilename", true);

        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();

            final Matcher matcher = getMatcherAgainstContent("(?is)<div[^<>]*?id=\"(?:dvifree|content_ifree)\"[^<>]*?>.+?</form>");
            if (!matcher.find()) throw new PluginImplementationException("Free download form not found");
            final HttpMethod httpMethod2 = getMethodBuilder(matcher.group()).setReferer(fileURL).setActionFromFormByIndex(1, true).toPostMethod();
            if (!makeRedirectedRequest(httpMethod2)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            String secondPageUrl = httpMethod2.getURI().toString();

            if (!getContentAsString().contains("\"dvifree\"")) {
                //Russian IPs may see a different page here, let's handle it
                final HttpMethod httpMethodR = getMethodBuilder().setReferer(secondPageUrl).setActionFromFormWhereActionContains("letitbit.net", true).toPostMethod();
                if (!makeRedirectedRequest(httpMethodR)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                secondPageUrl = httpMethodR.getURI().toString();
            }

            final MethodBuilder captchaBuilder = getMethodBuilder().setReferer(secondPageUrl).setActionFromFormByName("dvifree", true);
            final String captchaurl = getCaptchaImageURL();
            do {
                final HttpMethod captchaMethod = captchaBuilder.setParameter("cap", readCaptchaImage(captchaurl)).toPostMethod();
                if (!makeRedirectedRequest(captchaMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                captchatry++;
            } while (getContentAsString().contains("history.go(-1)"));

            final HttpMethod httpMethod3 = getMethodBuilder()
                    .setActionFromIFrameSrcWhereTagContains("name=\"topFrame\"")
                    .setReferer(secondPageUrl).toGetMethod();
            final String thirdPageUrl = httpMethod3.getURI().toString();
            if (!makeRedirectedRequest(httpMethod3)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "<span id=\"errt\">", "</span>") + 1);

            if (!makeRedirectedRequest(httpMethod3)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            final HttpMethod httpMethod4 = getMethodBuilder()
                    .setActionFromAHrefWhereATagContains("Your link to file download")
                    .setReferer(thirdPageUrl)
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod4)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getCaptchaImageURL() throws Exception {
        String s = getMethodBuilder().setBaseURL("http://letitbit.net").setActionFromImgSrcWhereTagContains("cap.php").getEscapedURI();
        logger.info("Captcha image URL: " + s);
        return s;
    }

    private String readCaptchaImage(final String captchaurl) throws Exception {
        final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(captchaurl);
        String captcha;
        switch (captchatry) {
            case 0:
                CaptchaReader cr = new CaptchaReader(captchaImage);
                captcha = cr.getWord();
                if (captcha != null) {
                    logger.info("Captcha - RickCL Captcha recognized: " + captcha);
                    break;
                }
            case 1:
                final BufferedImage croppedCaptchaImage = captchaImage.getSubimage(1, 1, captchaImage.getWidth() - 2, captchaImage.getHeight() - 2);
                captcha = PlugUtils.recognize(croppedCaptchaImage, "-C a-z-0-9");
                if (captcha != null) {
                    logger.info("Captcha - OCR recognized: " + captcha);
                    break;
                }
            default:
                captcha = getCaptchaSupport().askForCaptcha(captchaImage);
                if (captcha != null) {
                    logger.info("Captcha - Manual: " + captcha);
                }
                break;
        }
        if (captcha == null)
            throw new CaptchaEntryInputMismatchException();
        return captcha;
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        final String content = getContentAsString();
        if (content.contains("The page is temporarily unavailable")) {
            throw new ServiceConnectionProblemException("The page is temporarily unavailable");
        }
        if (content.contains("You must have static IP")) {
            throw new ServiceConnectionProblemException("You must have static IP");
        }
        if (content.contains("file was not found")
                || content.contains("\u043D\u0430\u0439\u0434\u0435\u043D")
                || content.contains("<title>404</title>")
                || (content.contains("Request file ") && content.contains(" Deleted"))) {
            throw new URLNotAvailableAnymoreException("The requested file was not found");
        }
    }

}