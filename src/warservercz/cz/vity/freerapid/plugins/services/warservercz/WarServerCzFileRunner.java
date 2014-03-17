package cz.vity.freerapid.plugins.services.warservercz;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
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
class WarServerCzFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(WarServerCzFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "<h1>", "</h1>");
        PlugUtils.checkFileSize(httpFile, content, "Velikost: <strong>", "</strong>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            Matcher matcher = PlugUtils.matcher("startFreeDownload\\(.+?,(.+?),(.+?)\\)", getContentAsString());
            if (!matcher.find()) {
                throw new PluginImplementationException("File id/wait time not found ");
            }
            final String fid = PlugUtils.unescapeHtml(matcher.group(1).trim()).replace("\"", "");
            final int waitTime = Integer.parseInt(PlugUtils.unescapeHtml(matcher.group(2).trim()).replace("\"", ""));

            matcher = PlugUtils.matcher("<script type\\s*=\\s*[\"']text/javascript[\"'] src\\s*=\\s*[\"'](.+?/warserver.+?\\.js.*?)[\"']>", getContentAsString());
            if (!matcher.find()) {
                throw new PluginImplementationException("WarServer javascript file not found");
            }
            final String jsFile = matcher.group(1);
            setFileStreamContentTypes(new String[0], new String[]{"application/x-javascript"});
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(jsFile)
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            matcher = PlugUtils.matcher("var link\\s*=\\s*[\"'](.+?)[\"']", getContentAsString());
            if (!matcher.find()) {
                throw new PluginImplementationException("Download link not found");
            }
            final String downloadLink = matcher.group(1) + fid;
            downloadTask.sleep(waitTime + 1);
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadLink)
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
        if (contentAsString.contains("Soubor nenalezen")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}