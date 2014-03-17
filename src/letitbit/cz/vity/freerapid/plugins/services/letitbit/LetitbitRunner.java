package cz.vity.freerapid.plugins.services.letitbit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
 */
public class LetitbitRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LetitbitRunner.class.getName());

    protected void setLanguageCookie() {
        addCookie(new Cookie(".letitbit.net", "lang", "en", "/", 86400, false));
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setLanguageCookie();
        final HttpMethod httpMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    protected void checkNameAndSize() throws Exception {
        try {
            final String name = PlugUtils.getStringBetween(getContentAsString(), "<span class=\"file-info-name\">", "</span>");
            httpFile.setFileName(PlugUtils.unescapeHtml(name).trim());
            PlugUtils.checkFileSize(httpFile, getContentAsString(), "<span class=\"file-info-size\">[", "]</span>");
        } catch (Exception e) {
            final String name = PlugUtils.getStringBetween(getContentAsString(), ": <span>", "</span>");
            httpFile.setFileName(PlugUtils.unescapeHtml(name).trim());
            PlugUtils.checkFileSize(httpFile, getContentAsString(), "[<span>", "</span>]");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        setLanguageCookie();
        setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
        HttpMethod httpMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
            List<String> urls = new LetitbitApi(client).getDownloadUrls(fileURL);
            if (urls == null) {
                for (int i = 1; i <= 3; i++) {
                    if (!postFreeForm()) {
                        if (i == 1) {
                            throw new PluginImplementationException("Free download button not found");
                        }
                        break;
                    }
                    logger.info("Posted form #" + i);
                }
                downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "seconds =", ";") + 1);
                String content = handleCaptcha();
                logger.info("Ajax response: " + content);
                if (content.contains("[\"")) {
                    content = PlugUtils.getStringBetween(content, "[", "]").replaceAll("(\\\\|\")", "");
                    urls = Arrays.asList(content.split(","));
                } else {
                    urls = Arrays.asList(content);
                }
            }
            httpMethod = getGetMethod(getFinalUrl(urls));
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
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
                || (content.contains("Request file ") && content.contains(" Deleted"))
                || content.contains("File not found")
                || content.contains("<body><h1>Error</h1></body>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private boolean postFreeForm() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("(?is)(<form\\b.+?</form>)");
        while (matcher.find()) {
            final String content = matcher.group(1);
            if (content.contains("md5crypt") && !content.contains("/sms/check")) {
                final HttpMethod method = getMethodBuilder(content).setActionFromFormByIndex(1, true).toPostMethod();
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
                return true;
            }
        }
        return false;
    }

    private String getFinalUrl(final List<String> urls) throws Exception {
        for (final String url : urls) {
            final HttpMethod method = getGetMethod(url + "&check=1");
            logger.info("Checking URL: " + method.getURI().toString());
            method.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            method.removeRequestHeader("Referer");
            if (!makeRequest(method)) {
                checkProblems();
                throw new PluginImplementationException();
            }
            if (method.getStatusCode() == HttpStatus.SC_OK) {
                logger.info("Final URL: " + url);
                return url;
            }
        }
        throw new ServiceConnectionProblemException("Final URL not found");
    }

    private String handleCaptcha() throws Exception {
        final String rcKey = "6Lc9zdMSAAAAAF-7s2wuQ-036pLRbM0p8dDaQdAM";
        final String rcControl = PlugUtils.getStringBetween(getContentAsString(), "var recaptcha_control_field = '", "';");
        while (true) {
            final ReCaptcha rc = new ReCaptcha(rcKey, client);
            final String captcha = getCaptchaSupport().getCaptcha(rc.getImageURL());
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            }
            rc.setRecognized(captcha);
            final HttpMethod method = rc.modifyResponseMethod(getMethodBuilder()
                    .setAjax()
                    .setAction("/ajax/check_recaptcha.php"))
                    .setParameter("recaptcha_control_field", rcControl)
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final String content = getContentAsString().trim();
            if (content.contains("error_free_download_blocked")) {
                throw new ErrorDuringDownloadingException("You have reached the daily download limit");
            } else if (!content.contains("error_wrong_captcha")) {
                return content;
            }
        }
    }

}