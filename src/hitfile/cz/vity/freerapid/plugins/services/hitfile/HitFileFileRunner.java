package cz.vity.freerapid.plugins.services.hitfile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class HitFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(HitFileFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        addCookie(new Cookie("hitfile.net", "user_lang", "en", "/", 86400, false));
        checkUrl();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkUrl() {
        if (!fileURL.contains("download/free")) {
            fileURL = fileURL.replaceFirst("hitfile.net", "hitfile.net/download/free");
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher fileNameMatcher = PlugUtils.matcher("Download file\\s*<br>.*>(.+)</span>", content);
        if (!fileNameMatcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(fileNameMatcher.group(1));
        final Matcher fileSizeMatcher = PlugUtils.matcher("File size:</b>\\s*(.+)</div>", content);
        if (!fileSizeMatcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSizeMatcher.group(1)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie("hitfile.net", "user_lang", "en", "/", 86400, false));
        checkUrl();
        logger.info("Starting download in TASK " + fileURL);

        HttpMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkFileProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            while (getContentAsString().contains("/captcha/")) {
                MethodBuilder builder = getMethodBuilder()
                        .setActionFromFormWhereActionContains("#", true)
                        .setReferer(fileURL).setAction(fileURL);
                String content = getContentAsString();
                // check 4 & complete captcha
                if (content.contains("Type the characters you see")) {
                    stepCaptcha(builder);
                }
                stepCaptcha(builder);
                if (!makeRedirectedRequest(builder.toPostMethod())) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            }

            Matcher matcher = getMatcherAgainstContent("\"minLimit\"\\s*?:\\s*?\"(.+?)\"");
            if (matcher.find()) {
                downloadTask.sleep(Integer.parseInt(matcher.group(1)) + 1);
            }


            matcher = getMatcherAgainstContent("\"fileId\"\\s*?:\\s*?\"(.+?)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("File ID not found");
            }
            method = getMethodBuilder()
                    .setReferer(method.getURI().toString())
                    .setAction(getRequestUrl(matcher.group(1)))
                    .toGetMethod();
            method.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            method = getMethodBuilder()
                    .setReferer(method.getURI().toString())
                    .setActionFromAHrefWhereATagContains("Download")
                    .toGetMethod();

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found") || getContentAsString().contains("Probably it was deleted")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("The file is not available now because of technical problems")) {
            throw new ServiceConnectionProblemException("The file is not available now because of technical problems");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        checkFileProblems();
        if (contentAsString.contains("Unable to Complete Request")) {
            throw new ServiceConnectionProblemException("Unable to Complete Request");
        }
        if (contentAsString.contains("You have reached the limit")) {
            final int waitTime = PlugUtils.getNumberBetween(contentAsString, "<span id='timeout'>", "</span>");
            throw new YouHaveToWaitException("Download limit reached", waitTime);
        }
    }


    private void stepCaptcha(MethodBuilder method) throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();

        final String captchaImg = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getEscapedURI();
        logger.info(">>>>>>>>>>>>> Captcha URL " + captchaImg);
//        captchaSupport.getCaptchaImage(captchaSrc);

        final String captchaTxt = captchaSupport.getCaptcha(captchaImg);
        if (captchaTxt == null) throw new CaptchaEntryInputMismatchException("No Input");
        logger.info("Manual captcha " + captchaTxt);

        method.setParameter("captcha_response", captchaTxt);
    }

    private String getRequestUrl(final String fileId) throws Exception {
        final String random = String.valueOf(1 + new Random().nextInt(100000));
        final byte[] bytes = (fileId + random).getBytes("ISO-8859-1");
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] ^= 101;
        }
        final String base64 = Base64.encodeBase64String(bytes).replace('/', '_');
        return "/download/getlinktimeout/" + fileId + "/" + random + "/" + base64;
    }
}