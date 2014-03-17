package cz.vity.freerapid.plugins.services.u115;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Meow
 */
class u115FileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(u115FileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<title>", " - 115\u7F51\u7EDCU\u76D8");
        PlugUtils.checkFileSize(httpFile, content, "\">\u6587\u4EF6\u5927\u5C0F\uFF1A", "</td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);

            final Matcher matcher = getMatcherAgainstContent("href=\"(http://.+?)\" onclick=\"sendMnvdToServer\\(\\);\"");
            if (matcher.find()) {
                final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1)).toGetMethod();
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                throw new PluginImplementationException("Download link not found");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("id=\"error_message\"") || contentAsString.contains("\u8BBF\u95EE\u7684\u9875\u9762\u4E0D\u5B58\u5728")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}