package cz.vity.freerapid.plugins.services.usua;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u2
 */
class UsUaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UsUaFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "<h2 class=\"red\">", "</h2");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(PlugUtils.getStringBetween(content, "</h2>", "<div id=\"message_download\">").replace("&nbsp;", " ").replace("Б", "B")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString().replace("\\/", "/");
            checkProblems();
            checkNameAndSize(contentAsString);
            downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "show_timer(", ");"));
            final HttpMethod httpMethod = getMethodBuilder(contentAsString)
                    .setReferer(fileURL)
                    .setActionFromFormWhereActionContains("/download", true)
                    .toHttpMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
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
        if (contentAsString.contains("Запрошенный вами файл не найден")||
                contentAsString.contains("Вероятно файл был удален с сервера")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}