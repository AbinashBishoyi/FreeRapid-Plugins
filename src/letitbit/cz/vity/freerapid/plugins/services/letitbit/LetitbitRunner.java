package cz.vity.freerapid.plugins.services.letitbit;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;
import java.util.logging.Logger;

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
        PlugUtils.checkName(httpFile, getContentAsString(), "target=\"_blank\"><span>", "</span>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "[<span>", "</span>]");
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

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormByName("ifree_form", true)
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            String pageUrl = httpMethod.getURI().toString();

            httpMethod = getMethodBuilder()
                    .setReferer(pageUrl)
                    .setActionFromFormByName("d3_form", true)
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            pageUrl = httpMethod.getURI().toString();

            // Russian IPs may see this different page here, handle it
            if (PlugUtils.find("action=\"http://s\\d+\\.letitbit\\.net/download3\\.php\"", getContentAsString())) {
                httpMethod = getMethodBuilder()
                        .setReferer(pageUrl)
                        .setActionFromFormWhereActionContains(".letitbit.net/download3.php", true)
                        .toPostMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                pageUrl = httpMethod.getURI().toString();
            }

            downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "seconds =", ";") + 1);

            final String url = handleCaptcha(pageUrl);

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
                || content.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
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
            if (!content.isEmpty()) {
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