package cz.vity.freerapid.plugins.services.kuaichuan;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Tommy Yang
 */
class KuaiRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(KuaiRunner.class.getName());
    private static KuaiCookieContainer cookieContainer = new KuaiCookieContainer();

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (isBinaryUrl()) {
            checkSizeAndName();
        } else if (makeRedirectedRequest(method)) {
            checkProblems();
            checkSizeAndName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);

        if (isBinaryUrl()) {
            // direct download.
            logger.info("Direct download from: " + fileURL);
            checkSizeAndName();
            Cookie[] loadedCookies = cookieContainer.getCookies(fileURL);
            client.getHTTPClient().getState().addCookies(loadedCookies);
            client.getHTTPClient().getParams().setBooleanParameter(DownloadClientConsts.IGNORE_ACCEPT_RANGES, true);
            method = getMethodBuilder()
                    .setAction(fileURL)
                    .toHttpMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ErrorDuringDownloadingException("Error starting downloading.");
            }
        } else if (makeRedirectedRequest(method)) {
            checkProblems();
            checkSizeAndName();

            Matcher downloadLinkMatcher = PlugUtils.matcher("file_url=\"([^\"]+)\"", getContentAsString());
            final List<URI> uriList = new LinkedList<URI>();
            while (downloadLinkMatcher.find()) {
                final String downloadUrl = downloadLinkMatcher.group(1);
                uriList.add(new URI(downloadUrl));
                cookieContainer.pushCookies(downloadUrl, getCookies(), fileURL);
            }
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkSizeAndName() throws ErrorDuringDownloadingException {
        if (isBinaryUrl()) {
            PlugUtils.checkName(httpFile, fileURL, ":8000/", "?");
        } else {
            PlugUtils.checkName(httpFile, getContentAsString(), "file_name=\"", "\"");
            PlugUtils.checkFileSize(httpFile, getContentAsString(), "c3\" style=\"width:244px;text-align:left;\">", "</span>");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException, IOException {
        if (getContentAsString().contains("URL was not found") || getContentAsString().contains("404 Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        } else if (getContentAsString().contains("请输入验证码以继续操作")) {
            logger.info(getCookies().toString());
            HttpMethod method = stepCaptcha();
            makeRedirectedRequest(method);
            logger.info(getContentAsString());
        }
    }

    private HttpMethod stepCaptcha() throws PluginImplementationException, FailedToLoadCaptchaPictureException {
        Matcher matcher = PlugUtils.matcher("http://verify2\\.xunlei\\.com/image[^\"]+", getContentAsString());
        if (!matcher.find())
            throw new PluginImplementationException("Cannot get captcha.");
        final String captcha = getCaptchaSupport().getCaptcha(matcher.group());
        final String shortKey = PlugUtils.getStringBetween(getContentAsString(), "value='", "' name=\"shortkey");
        return getMethodBuilder()
                .setReferer(fileURL)
                .setAction("/webfilemail_interface")
                .setParameter("v_code", captcha)
                .setParameter("shortkey", shortKey)
                .setParameter("ref", "")
                .setParameter("action", "check_verify")
                .toGetMethod();
    }

    private boolean isBinaryUrl() {
        return fileURL.contains("sendfile.vip.xunlei");
    }
}