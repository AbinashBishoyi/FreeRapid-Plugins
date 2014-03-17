package cz.vity.freerapid.plugins.services.yunfile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Stan
 */
class YunFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(YunFileFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "&nbsp;&nbsp;", "</h2>");
        PlugUtils.checkFileSize(httpFile, content, "<b>", "</b>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            final HttpMethod getMethodForDownPage = getMethodBuilder()
                    .setReferer(fileURL).setActionFromTextBetween("downpage_link\" href=\"", "\"")
                    .toGetMethod();
            if (!makeRedirectedRequest(getMethodForDownPage)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            final HttpMethod postMethodForFile = getMethodBuilder()
                    .setReferer(fileURL).setActionFromFormByName("down_from", true).toPostMethod();

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(postMethodForFile)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("\u6587\u4EF6\u4E0D\u5B58\u5728")) { // 文件不存在 {@see http://www.snible.org/java2/uni2java.html}
            throw new URLNotAvailableAnymoreException("File not found");
        }

        if (contentAsString.contains("down_interval")) {
            throw new YouHaveToWaitException("Waiting for next file.",
                    PlugUtils.getWaitTimeBetween(contentAsString,
                            "down_interval\" style=\"font-size: 28px; color: green;\">", "</span>",
                            TimeUnit.MINUTES));
        }
    }
}