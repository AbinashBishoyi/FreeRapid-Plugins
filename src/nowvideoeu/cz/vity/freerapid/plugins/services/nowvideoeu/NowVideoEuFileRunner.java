package cz.vity.freerapid.plugins.services.nowvideoeu;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.plugins.webclient.utils.ScriptUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URLDecoder;
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
        if (PlugUtils.find("(embed|player)\\.php\\?v=", fileURL)) {
            fileURL = fileURL.replaceFirst("http://.*?nowvideo\\.(.{2})/(embed|player)\\.php\\?v=(.+)", "http://www.nowvideo.$1/video/$3");
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

            String fileId = fileURL.replaceFirst("http://.+?/video/", "");
            String fileKey;
            String content = getContentAsString().contains(".file=") ? getContentAsString() : findParameterContent();
            Matcher matcher = PlugUtils.matcher("\\.filekey=[\"']?(.+?)[\"']?;", content);
            if (matcher.find()) {
                fileKey = matcher.group(1);
                if (fileKey.contains("\"") || fileKey.contains("'")) { //filekey is string
                    fileKey = fileKey.replace("\"", "").replace("'", "");
                } else { //filekey param is stored in variable
                    matcher = PlugUtils.matcher(String.format("var %s\\s*=\\s*[\"'](.+?)[\"']\\s*;", fileKey), content);
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Error parsing file key");
                    }
                    fileKey = matcher.group(1);
                }
            } else {
                throw new PluginImplementationException("File key not found");
            }

            HttpMethod httpMethod = getVideoMethod(getPlayerApiMethodBuilder(fileId, fileKey), true);
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                MethodBuilder mb = getPlayerApiMethodBuilder(fileId, fileKey)
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

    private MethodBuilder getPlayerApiMethodBuilder(String fileId, String fileKey) throws BuildMethodException {
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
            videoUrl = URLDecoder.decode(PlugUtils.getStringBetween(getContentAsString(), "url=", "&title="), "UTF-8");
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
        if (contentAsString.contains("file no longer exists") || contentAsString.contains("<h1>404 - Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("The file is being converted")) {
            throw new ServiceConnectionProblemException("The file is being converted");
        }
    }

    private String findParameterContent() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("eval([^\r\n]+)");
        if (!matcher.find()) {
            throw new PluginImplementationException("Parameters not found (1)");
        }
        String content = ScriptUtils.evaluateJavaScriptToString(matcher.group(1));

        String[] split = content.split("eval");
        if (split.length != 2) {
            throw new PluginImplementationException("Parameters not found (2)");
        }
        content = ScriptUtils.evaluateJavaScriptToString(split[1]);

        split = content.split("eval");
        if (split.length != 3) {
            throw new PluginImplementationException("Parameters not found (3)");
        }
        content = ScriptUtils.evaluateJavaScriptToString(split[2]);

        return content;
    }

}