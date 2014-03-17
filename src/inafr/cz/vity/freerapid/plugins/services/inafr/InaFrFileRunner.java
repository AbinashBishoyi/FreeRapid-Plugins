package cz.vity.freerapid.plugins.services.inafr;

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
 */
class InaFrFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(InaFrFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "<h2 class=\"titre-propre\">", "</h2>");
        httpFile.setFileName(httpFile.getFileName() + ".mp4");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            final String idNotice = PlugUtils.getStringBetween(getContentAsString(), "idNotice\":\"", "\"");
            final String swfUrl = "http://www.ina.fr" + PlugUtils.getStringBetween(getContentAsString(), "movieUrl\":\"", "\"").replace("\\/", "/");
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(swfUrl)
                    .setAction(String.format("http://www.ina.fr/player/infovideo/id_notice/%s/module_request/notice", idNotice))
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final String mediaUrl = PlugUtils.getStringBetween(getContentAsString(), "<Media>", "</Media>");
            httpMethod = getMethodBuilder()
                    .setReferer(swfUrl)
                    .setAction(mediaUrl)
                    .toGetMethod();
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
        if (contentAsString.contains("Page non trouvée")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}