package cz.vity.freerapid.plugins.services.freakshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Thumb, ntoskrnl
 */
class FreakShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FreakShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();

        waitForTime();
        HttpMethod httpMethod = getMethodBuilder()
                .setActionFromFormWhereActionContains("http://freakshare.net/files", true)
                .toHttpMethod();

        if (!makeRedirectedRequest(httpMethod))
            throw new ServiceConnectionProblemException();
        checkProblems();
        waitForTime();

        httpMethod = getMethodBuilder()
                .setActionFromFormWhereActionContains("http://freakshare.net/files", true)
                .toHttpMethod();
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void waitForTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("var\\s+?time\\s*?=\\s*?(\\d+(\\.\\d+)?)");
        if (matcher.find()) {
            final int wait = Double.valueOf(matcher.group(1)).intValue();
            if (wait > 0) {
                downloadTask.sleep(wait + 1);
            }
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<h1 class=\"box_heading\"[^<>]+?>(.+?) - (.+?)</");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name/size not found");
        }
        httpFile.setFileName(matcher.group(1));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("<h1[^<>]*>[^<>]*Error[^<>]*</h1>[^<>]*<div[^<>]*>([^<>]*)");
        if (matcher.find()) {
            String detail = matcher.group(1);
            if (PlugUtils.find("does.?n.?t\\s+exist", detail))
                throw new URLNotAvailableAnymoreException(detail);
            if (PlugUtils.find("can.?t\\s+download\\s+more\\s+th.n", detail))
                throw new YouHaveToWaitException(detail, 1800); // wait 30 minutes (as we have no way to tell how long)
            throw new NotRecoverableDownloadException(detail);
        }
    }

}
