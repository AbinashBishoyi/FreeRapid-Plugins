package cz.vity.freerapid.plugins.services.sharexxpgcombr;

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
class SharexXpgComBrFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SharexXpgComBrFileRunner.class.getName());

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
        if (fileURL.contains("beta.sharex.xpg.com.br")) { //beta.sharex.xpg.com.br
            final Matcher fileNameMatcher = PlugUtils.matcher("\"downinfo\">(.+?)</div>", content);
            if (!fileNameMatcher.find()) {
                throw new PluginImplementationException("File name not found");
            }
            httpFile.setFileName(fileNameMatcher.group(1).trim());
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else { //sharex.xpg.com.br
            final Matcher fileNameSizeMatcher = PlugUtils.matcher("\"downinfo\">(.+?)<b>(.+?)</b>", content);
            if (!fileNameSizeMatcher.find()) {
                throw new PluginImplementationException("File name and size not found");
            }
            httpFile.setFileName(fileNameSizeMatcher.group(1).trim());
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileNameSizeMatcher.group(2)));
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }
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
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("download_but.png")
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
        //if (contentAsString.contains("O conteúdo que você procurava não foi encontrado") || contentAsString.contains("404 Not Found")||contentAsString.contains("was not found on this server")) {
        if (contentAsString.contains("não foi encontrado") || contentAsString.contains("was not found on this server")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}