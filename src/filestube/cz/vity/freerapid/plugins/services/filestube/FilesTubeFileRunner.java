package cz.vity.freerapid.plugins.services.filestube;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class FilesTubeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilesTubeFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
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
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
            for (int i = 0; i < 5; i++) {//loop max 5 times
                final String content = getContentAsString();
                if (content.contains(">&nbsp;<")) {//stage 1
                    logger.info("Stage 1");
                    final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("&nbsp;").toGetMethod();
                    if (!makeRedirectedRequest(httpMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                } else if (content.contains("iframe_content")) {//stage 2
                    logger.info("Stage 2");
                    final String url = getMethodBuilder().setActionFromIFrameSrcWhereTagContains("").getAction();
                    logger.info("New URL: " + url);
                    httpFile.setNewURL(new URL(url));
                    httpFile.setPluginID("");
                    httpFile.setState(DownloadState.QUEUED);
                    return;
                } else {
                    checkProblems();
                    throw new PluginImplementationException("Unknown page content");
                }
            }
            checkProblems();
            throw new PluginImplementationException("Looped too many times, plugin broken?");
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        try {
            PlugUtils.checkName(httpFile, content, "<meta name=\"Keywords\" content=\"", ",");
            PlugUtils.checkFileSize(httpFile, content, "<td class=\"tright\">", "</td>");
        } catch (PluginImplementationException e) {
            logger.warning("File name/size not found");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Requested file was not found") || content.contains("Requested page was not found") || content.equals("Not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}