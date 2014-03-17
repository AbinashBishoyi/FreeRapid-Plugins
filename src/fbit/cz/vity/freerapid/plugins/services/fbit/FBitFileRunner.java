package cz.vity.freerapid.plugins.services.fbit;

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
class FBitFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FBitFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setPageEncoding("Windows-1251");
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
        PlugUtils.checkName(httpFile, getContentAsString(), "<td><div class=\"dwn_res_01\">Файл:</div></td> <td><div class=\"dwn_res_02\">", "</div></td>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<td><div class=\"dwn_res_01\">Размер:</div></td> <td><div class=\"dwn_res_02\">", "</div></td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        setPageEncoding("Windows-1251");
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
            setClientParameter(DownloadClientConsts.NO_CONTENT_LENGTH_AVAILABLE, true);
            method = getMethodBuilder().setActionFromFormByName("DownloadForm", true).toPostMethod();
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
        if (getContentAsString().contains("Файл не найден")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}