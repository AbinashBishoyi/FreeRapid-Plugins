package cz.vity.freerapid.plugins.services.getzilla;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class GetZillaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GetZillaFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        client.getHTTPClient().getParams().setContentCharset("UTF-8");
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
        PlugUtils.checkName(httpFile, content, "<h1>Скачать файл&nbsp;<span>&laquo;", "&raquo;</span>");
        final Matcher fileSizeMatcher = getMatcherAgainstContent("<div class=\"filesize.*?\">\\s*(.+?(?:Гб|Мб|Кб|байт)).*?\\s*<div");
        if (!fileSizeMatcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        final long fileSize = PlugUtils.getFileSizeFromString(fileSizeMatcher.group(1).replaceFirst("Гб", "gb").replace("Мб", "mb").replaceFirst("Кб", "kb").replaceFirst("байт", "b"));
        httpFile.setFileSize(fileSize);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        client.getHTTPClient().getParams().setContentCharset("UTF-8");
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(fileURL.replaceFirst("getzilla\\.net/files/", "getzilla.net/files/get/"))
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            final int waitTime = PlugUtils.getNumberBetween(getContentAsString(), "download_wait_time = \"", "\";") / 1000;
            downloadTask.sleep(waitTime + 1);
            httpMethod = getMethodBuilder()
                    .setReferer(httpMethod.getURI().toString())
                    .setAction(fileURL.replaceFirst("getzilla\\.net/files/", "getzilla.net/files/getUrl/"))
                    .setParameter("gold", "false")
                    .setParameter("_dc", String.valueOf(System.currentTimeMillis()))
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL.replaceFirst("getzilla\\.net/files/", "getzilla.net/files/get/"))
                    .setAction(getContentAsString())
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
        if (contentAsString.contains("Файл не найден")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("Превышен лимит одновременного скачивания в бесплатном режиме")) {
            throw new YouHaveToWaitException("Simultaneous download limit is exceeded in free mode", 5 * 60);
        }
    }

}