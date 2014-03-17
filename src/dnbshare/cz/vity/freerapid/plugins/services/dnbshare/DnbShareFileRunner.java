package cz.vity.freerapid.plugins.services.dnbshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class DnbShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DnbShareFileRunner.class.getName());

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

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<h1>", "</h1>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<em>Filesize</em>:", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormByName("dlform", true)
                    .setAction(fileURL)
                    .toPostMethod();
            downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "var c = ", ";") + 1);
            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("</em> not found") || content.contains("<h1>Not Found</h1>") || content.contains("was deleted")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}