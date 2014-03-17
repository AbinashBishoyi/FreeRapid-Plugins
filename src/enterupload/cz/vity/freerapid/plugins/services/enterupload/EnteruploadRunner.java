package cz.vity.freerapid.plugins.services.enterupload;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
 */

class EnteruploadRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(EnteruploadRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".enterupload.com", "lang", "english", "/", 86400, false));
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        PlugUtils.checkName(httpFile, getContentAsString(), "<h4>", "</h4>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<span>File size:", "</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".enterupload.com", "lang", "english", "/", 86400, false));
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("method_free", true)
                    .removeParameter("method_premium")
                    .setParameter("method_free", "Free+Download")
                    .toPostMethod();
            if (makeRedirectedRequest(method)) {
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromFormWhereTagContains("method_free", true)
                        .setParameter("method_free", "Free+Download")
                        .toPostMethod();
                final Matcher matcher = getMatcherAgainstContent("Wait <span[^<>]*?>(\\d+?)</span> seconds");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Waiting time not found");
                }
                downloadTask.sleep(Integer.parseInt(matcher.group(1)) + 1);
                if (makeRedirectedRequest(method)) {
                    method = getMethodBuilder()
                            .setReferer(fileURL)
                            .setActionFromFormWhereTagContains("btn_download", true)
                            .toGetMethod();
                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                } else {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ServiceConnectionProblemException, URLNotAvailableAnymoreException, YouHaveToWaitException, PluginImplementationException {
        if (getContentAsString().contains("not be found") || getContentAsString().contains("File Not Found") || getContentAsString().contains("No such file")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));

        }
        if (getContentAsString().contains("reached the download-limit")) {
            int timeToWait = 0;
            Matcher minute = PlugUtils.matcher("([0-9]+)//s*minute", getContentAsString());
            Matcher second = PlugUtils.matcher("([0-9]+)//s*second", getContentAsString());
            if (minute.find()) {
                timeToWait = Integer.parseInt(minute.group(1));
            }
            if (second.find()) {
                timeToWait += Integer.parseInt(second.group(1));
            }
            if (timeToWait == 0) timeToWait = 5 * 60;
            throw new YouHaveToWaitException("You have reached the download limit for free users", timeToWait + 1);
        }
    }

    @Override
    protected String getBaseURL() {
        return fileURL;
    }

}