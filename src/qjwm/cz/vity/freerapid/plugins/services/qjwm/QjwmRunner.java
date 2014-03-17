package cz.vity.freerapid.plugins.services.qjwm;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DefaultFileStreamRecognizer;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.interfaces.FileStreamRecognizer;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Tommy Yang
 */
class QjwmRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(QjwmRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        redirectToDownloadPage();
        final HttpMethod method = getGetMethod(fileURL);
        setPageEncoding("GB2312");
        if (makeRedirectedRequest(method)) {
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
        redirectToDownloadPage();
        HttpMethod method = getGetMethod(fileURL);
        setPageEncoding("GB2312");
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkSizeAndName();

            Matcher downloadLinkMatcher = PlugUtils.matcher("thunder_url = \"(.+?)\";", getContentAsString());
            if (!downloadLinkMatcher.find())
                throw new ErrorDuringDownloadingException("Cannot get download link from page.");
            final String downloadLink = downloadLinkMatcher.group(1);
            initDownload();
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadLink)
                    .toHttpMethod();

            logger.info("Start download from: " + method.getURI().toString());
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                logger.info("length: " + getContentAsString().length());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkSizeAndName() throws ErrorDuringDownloadingException {
        Matcher sizeMatcher = PlugUtils.matcher("文件大小:[^\\d]+([0-9\\. KMGT]+)", getContentAsString());
        if (!sizeMatcher.find())
            throw new ErrorDuringDownloadingException("Cannot read file size from page.");
        final String strFileSize = sizeMatcher.group(1) + "B";

        PlugUtils.checkName(httpFile, getContentAsString(), "qjwm.com</a> - ", "的下载地址");
        PlugUtils.getFileSizeFromString(strFileSize);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("URL was not found") || getContentAsString().contains("404 Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void redirectToDownloadPage() {
        final Matcher matcher1 = PlugUtils.matcher("(.+?)qjwm\\.com/down_([0-9]+)", fileURL);
        final Matcher matcher2 = PlugUtils.matcher("(.+?)qjwm\\.com/down.aspx?(.+)", fileURL);
        if (matcher1.find()) {
            // Can be redirected.
            fileURL = String.format("%sqjwm.com/download_%s.html", matcher1.group(1), matcher1.group(2));
            logger.info("Redirect url automatically to: " + fileURL);
        } else if (matcher2.find()) {
            fileURL = String.format("%sqjwm.com/download.aspx?%s", matcher2.group(1), matcher2.group(2));
            logger.info("Redirect url automatically to: " + fileURL);
        }
    }

    private void initDownload() {
        final String[] allowed = {"application"};
        getClientParameters().setUriCharset("GBK");
        FileStreamRecognizer recognizer = new DefaultFileStreamRecognizer(allowed, false);
        client.getHTTPClient().getParams().setParameter(DownloadClientConsts.FILE_STREAM_RECOGNIZER, recognizer);
        addCookie(new Cookie(".qjwm.com", "Flag", "UURealAntiLink", "/", 86400, false));
    }
}