package cz.vity.freerapid.plugins.services.nowvideoeu;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
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

    private void checkUrl() {
        if (fileURL.contains("embed.php?v=")) {
            fileURL = fileURL.replaceFirst("http://embed\\.nowvideo\\.(.{2})/embed\\.php\\?v=([a-z0-9]+)", "http://www.nowvideo.$1/video/$2");
        }
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkUrl();
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
        checkUrl();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final String fileId = PlugUtils.getStringBetween(getContentAsString(), "flashvars.file=\"", "\";");
            final String fileKey = PlugUtils.getStringBetween(getContentAsString(), "flashvars.filekey=\"", "\";");
            HttpMethod httpMethod = getVideoMethod(getNowVideoMethodBuilder(fileId, fileKey), true);
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                MethodBuilder mb = getNowVideoMethodBuilder(fileId, fileKey)
                        .setParameter("numOfErrors", "1")
                        .setParameter("errorCode", "404")
                        .setParameter("errorUrl", httpMethod.getURI().toString());
                if (!tryDownloadAndSaveFile(getVideoMethod(mb, false))) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private MethodBuilder getNowVideoMethodBuilder(String fileId, String fileKey) throws BuildMethodException {
        return getMethodBuilder()
                .setReferer(fileURL)
                .setAction("http://www.nowvideo.eu/api/player.api.php")
                .setParameter("cid3", "undefined")
                .setParameter("user", "undefined")
                .setParameter("cid2", "undefined")
                .setParameter("file", fileId)
                .setParameter("pass", "undefined")
                .setAndEncodeParameter("key", fileKey)
                .setParameter("cid", "1");
    }

    private HttpMethod getVideoMethod(MethodBuilder mb, boolean setFilename) throws Exception {
        if (!makeRedirectedRequest(mb.toGetMethod())) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        String videoUrl;
        try {
            videoUrl = PlugUtils.getStringBetween(getContentAsString(), "url=", "&title=");
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("Video URL not found");
        }
        if (setFilename) {
            String path = new URI(videoUrl).getPath();
            String fname = path.substring(path.lastIndexOf("/") + 1);
            String ext = fname.contains(".") ? fname.substring(fname.lastIndexOf(".")) : ".flv";
            httpFile.setFileName(httpFile.getFileName() + ext);
        }
        return getMethodBuilder()
                .setReferer(fileURL)
                .setAction(videoUrl)
                .toGetMethod();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file no longer exists")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}