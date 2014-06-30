package cz.vity.freerapid.plugins.services.videoweed;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.plugins.webclient.utils.ScriptUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author TommyTom, ntoskrnl
 */
class VideoWeedFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(VideoWeedFileRunner.class.getName());

    private void checkUrl() {
        if (fileURL.contains("embed.php?v=")) {
            fileURL = fileURL.replaceFirst("http://embed\\.videoweed\\.(es|com)/embed\\.php\\?v=([a-z0-9]+).*", "http://www.videoweed.$1/file/$2");
        }
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkUrl();
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
        String name = PlugUtils.getStringBetween(getContentAsString(), "<h1 class=\"text_shadow\">", "</h1>");
        final int index = name.lastIndexOf('.');
        if (index > 0) {
            name = name.substring(0, index) + ".flv";
        } else {
            name += ".flv";
        }
        httpFile.setFileName(name);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkUrl();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            String content = getContentAsString();
            if (!content.contains(".filekey=\"")) {
                content = findParameterContent();
            }
            final String file = PlugUtils.getStringBetween(content, ".file=\"", "\";");
            final String key = PlugUtils.getStringBetween(content, ".filekey=\"", "\";");
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://www.videoweed.es/api/player.api.php")
                    .setParameter("codes", "1")
                    .setParameter("key", key)
                    .setParameter("user", "undefined")
                    .setParameter("pass", "undefined")
                    .setParameter("file", file)
                    .toGetMethod();
            if (makeRedirectedRequest(method)) {
                final String url = PlugUtils.getStringBetween(getContentAsString(), "url=", "&");
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(URLDecoder.decode(url, "UTF-8") + "?client=FLASH")
                        .toGetMethod();
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("This file no longer exists on our servers") || content.contains("<h1>404 - Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("The file is being converted")) {
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