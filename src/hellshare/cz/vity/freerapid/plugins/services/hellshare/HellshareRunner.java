package cz.vity.freerapid.plugins.services.hellshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl, tong2shot
 */
class HellshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(HellshareRunner.class.getName());
    private final static int WAIT_TIME = 20;
    private final static int CAPTCHA_MAX = 5;
    private int captchaCounter = 1;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final int fid = getFId();
        final HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        checkNameAndSize();
        final Matcher matcher = getMatcherAgainstContent(String.format("<a href=\"(.+?/\\?do=(?:relatedFileDownloadButton-%d-showDownloadWindow|fileDownloadButton-showDownloadWindow))\"", fid));
        if (!matcher.find()) {
            throw new PluginImplementationException("Plugin is broken - showDownloadWindowAction not found");
        }
        final String showDownloadWindowAction = matcher.group(1);
        setFileStreamContentTypes(new String[0], new String[]{"application/json", "application/x-javascript"});
        HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(showDownloadWindowAction)
                .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        boolean saveSucceed = false;
        while (getContentAsString().contains("captcha")) {
            final String downloadWindowContent = getContentAsString().replace("\\\"", "\"").replace("\\/", "/");
            httpMethod = stepCaptcha(downloadWindowContent);
            if (saveSucceed = tryDownloadAndSaveFile(httpMethod)) {
                break;
            }
            final Header header = httpMethod.getResponseHeader("Location");
            if (header != null) {
                if (header.getValue().contains("/?error=")) //there is no popup shown, no captcha, no error message found, web UI doesn't respond
                    throw new ServiceConnectionProblemException("Server didn't respond");
            }
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
        }
        if (!saveSucceed) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private int getFId() throws PluginImplementationException {
        final int fid;
        final Matcher fidMatcher = PlugUtils.matcher("http://(?:www\\.)?download\\.hellshare\\.[a-z]{2,3}/[^/]+/(?:[^/]+/)?(\\d+)/?", fileURL);
        if (fidMatcher.find()) {
            fid = Integer.parseInt(fidMatcher.group(1));
        } else {
            throw new PluginImplementationException("File id not found");
        }
        return fid;
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("<h1 id=\"filename\".*?>(.+?)</h1>");
        if (!matcher.find()) {
            throw new PluginImplementationException("Filename not found");
        }
        httpFile.setFileName(matcher.group(1).trim());
        matcher = getMatcherAgainstContent("<strong id=\"FileSize_master\".*?>(.+?)</strong>");
        if (!matcher.find()) {
            throw new PluginImplementationException("Filesize not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1).replace("&nbsp;", " ")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private HttpMethod stepCaptcha(String downloadWindowContent) throws Exception {
        if (!downloadWindowContent.contains("captcha")) {
            throw new YouHaveToWaitException("Neurčité omezení", 4 * WAIT_TIME);
        }
        final String captchaURL = getMethodBuilder(downloadWindowContent)
                .setActionFromImgSrcWhereTagContains("captcha-img")
                .getAction();
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captcha;
        //captchaCounter = CAPTCHA_MAX + 1; //for testing purpose
        if (captchaCounter <= CAPTCHA_MAX) {
            final BufferedImage captchaImage = prepareCaptchaImage(captchaSupport.getCaptchaImage(captchaURL));
            captcha = new CaptchaRecognizer().recognize(captchaImage);
            logger.info("Attempt " + captchaCounter + " of " + CAPTCHA_MAX + ", OCR recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaURL);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }
        final MethodBuilder method = getMethodBuilder(downloadWindowContent).setActionFromFormWhereTagContains("captcha-img", true).setParameter("captcha", captcha);
        return method.toPostMethod();
    }

    private BufferedImage prepareCaptchaImage(final BufferedImage input) {
        final BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        final Graphics g = output.getGraphics();
        g.setXORMode(Color.WHITE);
        g.drawImage(input, 0, 0, null);
        return output;//almost too simple :)
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains(" limit free download")) {
            throw new YouHaveToWaitException("Dnešní limit free downloadů jsi vyčerpal", 60);
        }
        if (content.contains("Soubor nenalezen")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Soubor nenalezen</b><br>"));
        }
        if (content.contains(" free download|Na serveri")) {
            throw new YouHaveToWaitException("Na serveru jsou využity všechny free download sloty", WAIT_TIME);
        }
        if (content.contains("Stahujete soub")) {
            throw new YouHaveToWaitException("Na serveru jsou využity všechny free download sloty", WAIT_TIME);
        }
        if (content.contains("exceeded your today's limit for free download") || content.contains("Server load: 100%") || content.contains("Využití serveru: 100%")) {
            throw new YouHaveToWaitException("You exceeded your today's limit for free download. You can download only 1 files per 24 hours.", 10 * 60);
        }
        if (content.contains("HellShare is unavailable")) {
            throw new PluginImplementationException("HellShare is unavailable in your country");
        }
    }
}