package cz.vity.freerapid.plugins.services.nowvideoeu;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class NowVideoEuFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NowVideoEuFileRunner.class.getName());

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

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<h4>(.+?)</h4>\\s*<p");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        final String filename = matcher.group(1).trim();
        logger.info("File name : " + filename);
        httpFile.setFileName(filename);
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
                    .setAction("http://www.nowvideo.eu/api/player.api.php")
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
                    .setAction(downloadUrl)
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
        if (contentAsString.contains("file no longer exists")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}