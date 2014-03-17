package cz.vity.freerapid.plugins.services.letitbit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;

import java.net.URL;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
 */
class LetitbitRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LetitbitRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".letitbit.net", "lang", "en", "/", 86400, false));
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
        final String name = PlugUtils.getStringBetween(getContentAsString(), "<span class=\"file-info-name\">", "</span>");
        httpFile.setFileName(PlugUtils.unescapeHtml(name).trim());
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<span class=\"file-info-size\">[", "]</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".letitbit.net", "lang", "en", "/", 86400, false));
        setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);

        HttpMethod httpMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
            String pageUrl = fileURL;

            String url = new LetitbitApi(client).getDownloadUrl(fileURL);

            if (url == null) {
                for (int i = 1; i <= 3; i++) {
                    if (!postFreeForm()) {
                        break;
                    }
                    logger.info("Posted form #" + i);
                }

                downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "seconds =", ";") + 1);

                url = handleCaptcha(pageUrl);

                logger.info("Ajax response : " + url);

                if (url.contains("[\"")) {
                    url = PlugUtils.getStringBetween(url, "[", "]").replaceAll("\\\\", "");
                    final StringTokenizer st = new StringTokenizer(url, ",");
                    while (st.hasMoreTokens()) {
                        String testUrl = st.nextToken().replaceAll("\"", "");
                        logger.info("Url match : " + testUrl);
                        httpMethod = getGetMethod(testUrl + "&check=1");
                        logger.info("Url to be checked : " + httpMethod.getURI().toString());
                        httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
                        if (!makeRequest(httpMethod)) {
                            checkProblems();
                            throw new PluginImplementationException();
                        }
                        if (httpMethod.getStatusCode() == HttpStatus.SC_OK) {
                            url = testUrl;
                            break;
                        }
                    }
                }
            }

            logger.info("Final URL : " + url);

            httpMethod = getMethodBuilder()
                    .setReferer(pageUrl)
                    .setAction(url)
                    .toGetMethod();
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

    private String handleCaptcha(final String pageUrl) throws Exception {
        final String baseUrl = "http://" + new URL(pageUrl).getHost();
        while (true) {
            final String captchaUrl = "/captcha_new.php?rand=" + (int) Math.floor(100000 * Math.random());
            HttpMethod method = getMethodBuilder()
                    .setReferer(pageUrl)
                    .setBaseURL(baseUrl)
                    .setAction(captchaUrl)
                    .toGetMethod();
            final String captcha = getCaptcha(method);
            method = getMethodBuilder()
                    .setReferer(pageUrl)
                    .setBaseURL(baseUrl)
                    .setAction("/ajax/check_captcha.php")
                    .setParameter("code", captcha)
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

    private String getCaptcha(final HttpMethod method) throws Exception {
        final String captcha = getCaptchaSupport().askForCaptcha(getCaptchaSupport().loadCaptcha(client.makeRequestForFile(method)));
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        return captcha;
    }

}