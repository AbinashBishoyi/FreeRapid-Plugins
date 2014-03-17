package cz.vity.freerapid.plugins.services.letitbit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
 */
class LetitbitRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LetitbitRunner.class.getName());
    private String secondPageUrl, thirdPageUrl;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".letitbit.net", "lang", "en", "/", 86400, false));
        setPageEncoding("Windows-1251");
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        final String contentAsString = getContentAsString();
        PlugUtils.checkFileSize(httpFile, contentAsString, "File size::</span>", "</h1>");
        PlugUtils.checkName(httpFile, contentAsString, "File::</span>", "</h1>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".letitbit.net", "lang", "en", "/", 86400, false));
        client.getHTTPClient().getParams().setBooleanParameter("dontUseHeaderFilename", true);

        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();

            final MethodBuilder methodBuilder2 = getMethodBuilder().setActionFromFormByName("dvifree", true);
            secondPageUrl = methodBuilder2.getAction();
            final HttpMethod httpMethod2 = methodBuilder2.toPostMethod();
            if (!makeRedirectedRequest(httpMethod2)) {
                checkProblems();
                throw new PluginImplementationException("Download link 1 issue");
            }

            boolean useOCR = true;
            if (getContentAsString().contains("cap.php")) {
                while (getContentAsString().contains("cap.php") || getContentAsString().contains("javascript:history.go(-1);")) {
                    stepCaptcha(useOCR);
                    if (getContentAsString().contains("javascript:history.go(-1);")) {
                        logger.info("Wrong captcha");
                        useOCR = false;
                        makeRedirectedRequest(httpMethod2);
                    }
                }
            } else throw new PluginImplementationException("Download link 2 not found");
            logger.info("Captcha OK");

            Matcher matcher = getMatcherAgainstContent("<frame src=\"(http://s\\d.letitbit\\.net/tmpl/tmpl_frame_top\\.php\\?link=)\" name=\"topFrame\"");
            if (matcher.find()) {
                String actionURL = matcher.group(1);
                final MethodBuilder methodBuilder3 = getMethodBuilder()
                        .setMethodAction(actionURL)
                        .setReferer(secondPageUrl);
                thirdPageUrl = methodBuilder3.getAction();
                final HttpMethod httpMethod3 = methodBuilder3.toGetMethod();
                if (!makeRedirectedRequest(httpMethod3)) {
                    checkProblems();
                    throw new PluginImplementationException("Download link 3 issue");
                }

                downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "<span id=\"errt\">", "</span>") + 1);

                if (!makeRedirectedRequest(httpMethod3)) {
                    checkProblems();
                    throw new PluginImplementationException("Download link 4 issue");
                }

                if (!getContentAsString().contains("Your link to file download"))
                    throw new PluginImplementationException("Some waiting problem");

            } else throw new PluginImplementationException("Download link 3 not found");

            final HttpMethod httpMethod4 = getMethodBuilder()
                    .setActionFromAHrefWhereATagContains("Your link to file download")
                    .setReferer(thirdPageUrl)
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod4)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void stepCaptcha(boolean useOCR) throws Exception {
        try {
            String captcha;
            if (useOCR) captcha = readCaptchaImage();
            else captcha = getCaptchaSupport().getCaptcha(getCaptchaImageURL());
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            HttpMethod postMethod = getMethodBuilder()
                    .setActionFromFormWhereActionContains("download", true)
                    .setReferer(secondPageUrl)
                    .setParameter("cap", captcha).setParameter("fix", "1")
                    .toPostMethod();
            if (!makeRequest(postMethod)) {
                checkProblems();
                throw new PluginImplementationException("Captcha post issue");
            }
        } catch (BuildMethodException e) {
            logger.warning(e.getMessage());
            throw new PluginImplementationException("Captcha post issue");
        }
    }

    private String getCaptchaImageURL() throws Exception {
        String s = getMethodBuilder().setActionFromImgSrcWhereTagContains("cap.php").getAction();
        logger.info("Captcha - image " + s);
        return s;
    }

    private String readCaptchaImage() throws Exception {
        String s = getCaptchaImageURL();
        String captcha;
        final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(s);
        final BufferedImage croppedCaptchaImage = captchaImage.getSubimage(1, 1, captchaImage.getWidth() - 2, captchaImage.getHeight() - 2);
        captcha = PlugUtils.recognize(croppedCaptchaImage, "-C a-z-0-9");
        if (captcha != null) {
            logger.info("Captcha - OCR recognized " + captcha);
        } else captcha = "";
        captchaImage.flush();//askForCaptcha uvolnuje ten obrazek, takze tady to udelame rucne
        return captcha;

    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        final String content = getContentAsString();
        if (content.contains("The page is temporarily unavailable")) {
            throw new YouHaveToWaitException("The page is temporarily unavailable", 60 * 2);
        }
        if (content.contains("You must have static IP")) {
            throw new YouHaveToWaitException("You must have static IP, retrying...", 60 * 2);
        }
        if (content.contains("file was not found") || content.contains("\u043D\u0430\u0439\u0434\u0435\u043D") || content.contains("<title>404</title>")) {
            throw new URLNotAvailableAnymoreException("The requested file was not found");
        }
    }

}