package cz.vity.freerapid.plugins.services.bitupload;

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
class BitUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BitUploadFileRunner.class.getName());

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
        final Matcher matcher = getMatcherAgainstContent("<p title=\"[^<>]+?\">\\s*([^<>]+?\\s*)<br\\s*/>\\s*<strong>([^<>]+?)</strong>");
        if (!matcher.find()) throw new PluginImplementationException("Filename and filesize not found");
        httpFile.setFileName(matcher.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2).trim()));
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
            final String vfid = PlugUtils.getStringBetween(getContentAsString(), "var vfid = '", "';");
            downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "var delay =", ";"));
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://bitupload.com/hosting/download/getlink")
                    .setParameter("action", "getLink")
                    .setParameter("token", "undefined")
                    .setParameter("vfid", vfid)
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormByIndex(1, true)
                    .toPostMethod();
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
        if (contentAsString.contains("file not exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}