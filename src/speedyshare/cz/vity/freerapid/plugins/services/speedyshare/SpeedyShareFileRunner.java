package cz.vity.freerapid.plugins.services.speedyshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class SpeedyShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SpeedyShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod method = getGetMethod(fileURL);
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
        PlugUtils.checkName(httpFile, content, "<title>", "</title>");
        PlugUtils.checkFileSize(httpFile, content, "File size ", ", ");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            final Matcher matcher = getMatcherAgainstContent("href=\"(http://www\\.speedyshare\\.com/data/\\d+?/(\\d+?)/\\d+?/([^/]+?))\">");
            if (!matcher.find()) throw new PluginImplementationException("Download link not found");

            if (matcher.find(matcher.end())) { //multiple files
                httpFile.setFileName(httpFile.getFileName() + " (multiple files)");

                int start = 0;
                final List<URI> uriList = new LinkedList<URI>();
                while (matcher.find(start)) {
                    String link = "http://www.speedyshare.com/files/" + matcher.group(2) + "/" + matcher.group(3);
                    try {
                        uriList.add(new URI(link));
                    } catch (URISyntaxException e) {
                        LogUtils.processException(logger, e);
                    }
                    start = matcher.end();
                }
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);

            } else { //single file
                matcher.find(0);
                final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1)).toGetMethod();

                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new PluginImplementationException();
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Not Found") || content.contains("Not valid anymore") || content.contains("The file has been deleted") || !content.contains("<font class=resultlink>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("The one-hour limit")) {
            throw new YouHaveToWaitException("The one-hour limit for this download has been exceeded", 15 * 60);
        }
    }

}