package cz.vity.freerapid.plugins.services.qjwm;

import cz.vity.freerapid.plugins.exceptions.BuildMethodException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Tommy Yang
 */
class QjwmRunner extends XFileSharingRunner {
    private final static Logger logger = Logger.getLogger(QjwmRunner.class.getName());

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        super.checkFileProblems();
        final String content = getContentAsString();
        if (content.contains("www.safedog.cn")) {
            throw new ServiceConnectionProblemException("Proxy download forbidden");
        }
    }

    @Override
    protected void setLanguageCookie() throws Exception {
        addCookie(new Cookie(getCookieDomain(), "lang", "zh-cn", "/", 86400, false));
        setPageEncoding("GB2312");
    }

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new QjwmFileNameHandlerA());
        fileNameHandlers.add(0, new QjwmFileNameHandlerB());
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new QjwmFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add(0, "thunder_url = ");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        List<String> regex = super.getDownloadLinkRegexes();
        regex.add(0, "thunder_url = \"(.+?)\";");
        return regex;
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws BuildMethodException {
        final String old = fileURL;
        redirectToDownloadPage();
        return getMethodBuilder()
                .setReferer(old)
                .setAction(fileURL);
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

    @Override
    protected boolean tryDownloadAndSaveFile(HttpMethod method) throws Exception {
        getClientParameters().setUriCharset("GBK");
        setFileStreamContentTypes("application");
        addCookie(new Cookie(".qjwm.com", "Flag", "UURealAntiLink", "/", 86400, false));

        return super.tryDownloadAndSaveFile(method);
    }
}