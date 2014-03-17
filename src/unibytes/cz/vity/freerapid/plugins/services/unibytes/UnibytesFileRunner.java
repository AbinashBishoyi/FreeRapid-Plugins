package cz.vity.freerapid.plugins.services.unibytes;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.util.URIUtil;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author RickCL
 */
class UnibytesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UnibytesFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".unibytes.com", "lang", "en", "/", 86400, false));
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "<span title=\"", "\"");
        final long fileSize = PlugUtils.getFileSizeFromString(PlugUtils.getStringBetween(content, "(", ")</h3>").replace(",", "").trim());
        httpFile.setFileSize(fileSize);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".unibytes.com", "lang", "en", "/", 86400, false));
        HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        checkNameAndSize();
        final Matcher match = PlugUtils.matcher("<a href=\"([^\"]+?)\"[^>]*?btn_red[^>]*?>Free", getContentAsString());
        if (!match.find())
            throw new PluginImplementationException("Free download page not found");
        MethodBuilder methodBuilder = getMethodBuilder().setReferer(fileURL).setAction(match.group(1));
        methodBuilder.setAction(URIUtil.encodeQuery(methodBuilder.getAction()));
        method = methodBuilder.toHttpMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        final String timerURL = method.getURI().toString();
        /* skipped waitTime as in browser
        int waitTime = PlugUtils.getWaitTimeBetween(getContentAsString(), "var timerRest = ", ";", TimeUnit.SECONDS);
        downloadTask.sleep(waitTime);
        */
        methodBuilder = getMethodBuilder().setReferer(timerURL).setActionFromAHrefWhereATagContains("Download");
        methodBuilder.setAction(URIUtil.decode(methodBuilder.getAction()));
        method = methodBuilder.toGetMethod();
        setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true); //they trim filename
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws Exception {
        if (getContentAsString().contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found or removed");
        }
        if (getContentAsString().contains("Somebody else is already downloading")) {
            throw new ServiceConnectionProblemException("Somebody else is already downloading using your IP-address");
        }
        if (getContentAsString().contains("Try to download file later")) {
            int waitTime = PlugUtils.getWaitTimeBetween(getContentAsString(), "<span id=\"guestDownloadDelayValue\">", "</span>", TimeUnit.MINUTES);
            throw new YouHaveToWaitException("Try to download file later or get the VIP-account on our service. Wait for " + waitTime, waitTime);
        }
    }

}