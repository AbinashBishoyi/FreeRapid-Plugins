package cz.vity.freerapid.plugins.services.shareflare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.letitbit.LetitbitApi;
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


/**
 * @author RickCL, ntoskrnl
 */
class ShareflareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShareflareRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".shareflare.net", "lang", "en", "/", 86400, false));
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        PlugUtils.checkName(httpFile, getContentAsString(), "File: <span>", "</span>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "[<span>", "</span>]");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".shareflare.net", "lang", "en", "/", 86400, false));
        setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);

        HttpMethod httpMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
            final String content = getContentAsString();
            String pageUrl = fileURL;

            String url = new LetitbitApi(client).getDownloadUrl(fileURL);

            if (url == null) {
                httpMethod = getMethodBuilder(content)
                        .setReferer(pageUrl)
                        .setActionFromFormByName("fast_download_form", true)
                        .setAction(fileURL)
                        .toPostMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                pageUrl = httpMethod.getURI().toString();

                httpMethod = getMethodBuilder()
                        .setReferer(pageUrl)
                        .setActionFromFormByName("dvifree", true)
                        .toPostMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                pageUrl = httpMethod.getURI().toString();

                httpMethod = getMethodBuilder()
                        .setReferer(pageUrl)
                        .setActionFromFormByName("d3_form", true)
                        .toPostMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                pageUrl = httpMethod.getURI().toString();

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
        //  May produce false positives, eg. if the filename contains the word "Error".

        if (content.contains("You can wait download for")) {
            int wait = PlugUtils.getNumberBetween(getContentAsString(), "You can wait download for", "minutes");
            throw new YouHaveToWaitException(String.format("You could download your next file in %s minutes", (wait)), (wait * 60 + 1));

        }
        if (content.contains("You are currently downloading..")) {
            throw new ServiceConnectionProblemException(String.format("Your IP address is already downloading a file. Please wait until the download is completed."));
        }
        if (content.contains("File not found") || content.contains("deleted for abuse") || content.contains("<h1>404 Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("The requested file was not found");
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