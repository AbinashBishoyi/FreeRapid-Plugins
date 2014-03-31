package cz.vity.freerapid.plugins.services.unibytes;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

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
        final Matcher match = PlugUtils.matcher("Download file:<.+?>\\s+?(.+?)\\s+?<.+?>\\((.+?)\\)<", content);
        if (!match.find())
            throw new PluginImplementationException("File name/size not found");
        httpFile.setFileName(match.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(2)));
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
        method = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download").toGetMethod();
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