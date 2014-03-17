package cz.vity.freerapid.plugins.services.filedais;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FileDaisFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    private String fileLocUrl = fileURL;

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        return super.getXFSMethodBuilder().setReferer(fileLocUrl).setAction(fileLocUrl);
    }

    @Override
    public void run() throws Exception {
        setLanguageCookie();
        login();
        HttpMethod method = getGetMethod(fileURL);
        int httpStatus = client.makeRequest(method, true);
        if (httpStatus != 200) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        fileLocUrl = method.getURI().getURI();
        checkFileProblems();
        checkNameAndSize();
        checkDownloadProblems();
        for (int loopCounter = 0; ; loopCounter++) {
            if (loopCounter >= 8) {
                //avoid infinite loops
                throw new PluginImplementationException("Cannot proceed to download link");
            }
            final MethodBuilder methodBuilder = getXFSMethodBuilder();
            final int waitTime = getWaitTime();
            final long startTime = System.currentTimeMillis();
            stepPassword(methodBuilder);
            //skip the wait time if it is on the same page as a captcha of type ReCaptcha
            if (!stepCaptcha(methodBuilder)) {
                sleepWaitTime(waitTime, startTime);
            }
            method = methodBuilder.toPostMethod();
            client.makeRequest(method, true);
            if (checkDownloadPageMarker()) {
                //page containing download link
                final String downloadLink = getDownloadLinkFromRegexes();
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(downloadLink)
                        .toGetMethod();
                break;
            }
            checkDownloadProblems();
        }
        doDownload(method);
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "<a.*?href\\s?=\\s?['\"](http.+?" + Pattern.quote(httpFile.getFileName()) + ")['\"]");
        return downloadLinkRegexes;
    }

}