package cz.vity.freerapid.plugins.services.uploadhero;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class UploadHeroFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadHeroFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        addCookie(new Cookie(".uploadhero.co", "lang", "en", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkErrors();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkErrors();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<title>", " - UploadHero</title>");
        final Matcher mm = PlugUtils.matcher("Filesize\\s*:.*<strong>(.*)</strong>", content);
        if (!mm.find())
            throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(mm.group(1)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(".uploadhero.co", "lang", "en", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkErrors();
            checkProblems();
            checkNameAndSize(getContentAsString());//extract file name and size from the page

            if (!getContentAsString().contains("id=\"downloadforfreenow\"")) {
                checkProblems();
                throw new ServiceConnectionProblemException("Page not ready");
            }
            int counter = 0;
            while (!getContentAsString().contains("id=\"downloadforfreecount\"") && (counter++ < 5)) {
                if (!getContentAsString().contains("/captcha")) {
                    throw new ServiceConnectionProblemException("Captcha not found");
                } else {
                    MethodBuilder builder = getMethodBuilder().setAction(fileURL);
                    stepCaptcha(builder);
                    if (!makeRedirectedRequest(builder.toGetMethod())) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Problem loading download page");
                    }
                    checkProblems();
                }
            }
            if (!getContentAsString().contains("id=\"downloadforfreecount\"")) {
                checkProblems();
                throw new ServiceConnectionProblemException("Too many incorrect captcha codes entered");
            }
            downloadTask.sleep(1 + PlugUtils.getNumberBetween(getContentAsString(), "seconds:date.getSeconds()+", "});"));

            final Matcher mm = PlugUtils.matcher("magicomfg\\s*=\\s*'<a href=\"([^\"]+)\"", getContentAsString());
            if (!mm.find())
                throw new PluginImplementationException("Download link not found");
            method = getGetMethod(mm.group(1));

            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkErrors() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("The link file above no longer exists")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private void checkProblems() throws Exception {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Website overloaded")) {
            throw new YouHaveToWaitException("Website overloaded, please retry again later", 120);
        }
        if (contentAsString.contains("lightbox_block_download.php?")) {
            if (!makeRedirectedRequest(getGetMethod("http://uploadhero.com/lightbox_block_download.php")))
                throw new YouHaveToWaitException("You must wait between downloads", 300);
            int waitTime = 1;
            try {
                waitTime += 60 * PlugUtils.getNumberBetween(getContentAsString(), "<span id=\"minutes\">", "</span>");
                waitTime += PlugUtils.getNumberBetween(getContentAsString(), "<span id=\"seconds\">", "</span>");
            } catch (Exception e) { /* should only catch if <1sec left to wait */ }
            if (waitTime > 0) {
                logger.info("You must wait " + waitTime + " seconds");
                throw new YouHaveToWaitException("You must wait between downloads", waitTime);
            }
        }
    }

    private void stepCaptcha(MethodBuilder method) throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaImg = getMethodBuilder().setBaseURL("http://uploadhero.co").setActionFromImgSrcWhereTagContains("captcha").getEscapedURI();
        final String captchaTxt = captchaSupport.getCaptcha(captchaImg);
        if (captchaTxt == null) throw new CaptchaEntryInputMismatchException("No Input");

        method.setParameter("code", captchaTxt);
    }
}