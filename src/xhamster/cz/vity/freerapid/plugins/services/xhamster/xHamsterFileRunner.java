package cz.vity.freerapid.plugins.services.xhamster;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class xHamsterFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(xHamsterFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL();
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
        PlugUtils.checkName(httpFile, content, "<title>", " - xHamster.com");
        httpFile.setFileName(httpFile.getFileName() + ".flv");
        PlugUtils.checkFileSize(httpFile, content, "video (", ")");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkURL();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            final String file = PlugUtils.getStringBetween(getContentAsString(), "&file=", "&");   //flv
            //final String file = PlugUtils.getStringBetween(getContentAsString(), "file=\"", "\"");  //mp4
            final String videoURL;
            if (file.startsWith("http")) {
                videoURL = URLDecoder.decode(file, "UTF-8");
            } else {
                final String srv = PlugUtils.getStringBetween(getContentAsString(), "&srv=", "&");
                videoURL = URLDecoder.decode(srv + "/key=" + file, "UTF-8");
            }
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL.replace("88.208.24.43", "xhamster.com"))
                    .setAction(videoURL)
                    .toGetMethod();
            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
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
        if (contentAsString.contains("not found on this server") ||
                contentAsString.contains("This video was deleted")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("<title>Restricted access to video")) {
            throw new NotRecoverableDownloadException("Access to video is restricted");
        }
    }

    private void checkURL() {
        if (fileURL.startsWith("https"))
            fileURL = fileURL.replaceFirst("https", "http");
        fileURL = fileURL.replaceFirst("//.*?xhamster\\.com", "//88.208.24.43");
    }

}
