package cz.vity.freerapid.plugins.services.filerack;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
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
 * @author ntoskrnl
 */
class FileRackFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileRackFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, getContentAsString(), "Download File", "</");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "(<b>", "</b>)");
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

            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("Free Download", true)
                    .removeParameter("method_premium")
                    .setParameter("method_free", "Free Download")
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) throw new ServiceConnectionProblemException();

            httpMethod = getMethodBuilder()
                    .setReferer(httpMethod.getURI().toString())
                    .setActionFromFormByName("F1", true)
                    .toPostMethod();

            // We can skip both the captcha and the waiting time at the moment.
            // If the plugin breaks, check this first.

            //downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "<span id=\"countdown\">", "</span>") + 1);

            if (!makeRedirectedRequest(httpMethod)) throw new ServiceConnectionProblemException();

            httpMethod = getMethodBuilder()
                    .setReferer(httpMethod.getURI().toString())
                    .setActionFromAHrefWhereATagContains("alt=\"Download\"")
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
        final String content = getContentAsString();
        if (content.contains("Invalid Download Link") || content.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        final Matcher matcher = PlugUtils.matcher("Your IP address .+? is already downloaded a file. Please wait (\\d+?) minute", content);
        if (matcher.find()) {
            throw new YouHaveToWaitException("Waiting time between downloads", Integer.parseInt(matcher.group(1)) * 60);
        }
    }

}