package cz.vity.freerapid.plugins.services.yandexdisk;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class YandexDiskFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(YandexDiskFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            fileURL = getMethod.getURI().toString();
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<span class=\"b-text b-text_title\">", "</span>");
        httpFile.setFileName(PlugUtils.unescapeHtml(httpFile.getFileName()));
        PlugUtils.checkFileSize(httpFile, content, "<span class=\"b-text\">", ", uploaded");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            fileURL = method.getURI().toString();
            checkProblems();
            checkNameAndSize(getContentAsString());
            final String ckey = PlugUtils.getStringBetween(getContentAsString(), "\"ckey\":\"", "\"");
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://disk.yandex.com/handlers.jsx")
                    .setParameter("_ckey", ckey)
                    .setParameter("_name", "getLinkFileDownload")
                    .setParameter("hash", getHash())
                    .toPostMethod();
            method.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            method.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final String downloadURL = PlugUtils.replaceEntities(PlugUtils.getStringBetween(getContentAsString(), "\"url\":\"", "\""));
            logger.info(downloadURL);
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadURL)
                    .toGetMethod();
            setFileStreamContentTypes("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
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
        if (contentAsString.contains("was removed or not found")
                || contentAsString.contains("could not be found")
                || contentAsString.contains("bad formed path")
                || contentAsString.contains("resource not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String getHash() throws Exception {
        final Matcher matcher = PlugUtils.matcher("hash=(.+?)(?:&.*)?$", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing URL");
        }
        final String hash = URLDecoder.decode(matcher.group(1), "UTF-8");
        logger.info(hash);
        return hash;
    }

}