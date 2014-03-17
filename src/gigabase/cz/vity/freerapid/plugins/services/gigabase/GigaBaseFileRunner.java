package cz.vity.freerapid.plugins.services.gigabase;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class GigaBaseFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GigaBaseFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".gigabase.com", "lang", "en", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<span.*?\"fileName\".*?>(.+?)</span>\\s*\\((.+?)\\)");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name and size not found");
        }
        httpFile.setFileName(matcher.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2).replace(",", "").trim()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(".gigabase.com", "lang", "en", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("but_dnld_regular.jpg").toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error finding download link");
            }
            checkProblems();
            final HttpMethod dlMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download file").toGetMethod();

            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true); //they trim filename
            if (!tryDownloadAndSaveFile(dlMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
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