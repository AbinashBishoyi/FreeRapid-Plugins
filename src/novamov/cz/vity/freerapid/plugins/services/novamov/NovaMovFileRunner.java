package cz.vity.freerapid.plugins.services.novamov;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class NovaMovFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NovaMovFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (fileURL.matches("http://(?:www\\.)?novaup\\.com/.+")) {
            httpFile.setNewURL(new URL(fileURL.replaceFirst("novaup\\.com", "novamov.com")));
        }
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<h3>", "</h3>");
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
            final String fileId = PlugUtils.getStringBetween(getContentAsString(), "flashvars.file=\"", "\";");
            final String fileKey = PlugUtils.getStringBetween(getContentAsString(), "flashvars.filekey=\"", "\";");
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://www.novamov.com/api/player.api.php")
                    .setParameter("cid3", "undefined")
                    .setParameter("user", "undefined")
                    .setParameter("cid2", "undefined")
                    .setParameter("file", fileId)
                    .setParameter("pass", "undefined")
                    .setAndEncodeParameter("key", fileKey)
                    .setParameter("cid", "1")
                    .toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final String downloadUrl = PlugUtils.getStringBetween(getContentAsString(), "url=", "&title=");
            final String path = new URI(downloadUrl).getPath();
            final String fname = path.substring(path.lastIndexOf("/") + 1);
            final String ext = fname.contains(".") ? fname.substring(fname.lastIndexOf(".")) : ".flv";
            httpFile.setFileName(httpFile.getFileName() + ext);
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(URLDecoder.decode(downloadUrl, "UTF-8"))
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

    protected void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file no longer exists")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }
}