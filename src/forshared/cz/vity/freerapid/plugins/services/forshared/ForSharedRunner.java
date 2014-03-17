package cz.vity.freerapid.plugins.services.forshared;

import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
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
 * @author Alex, ntoskrnl
 */
class ForSharedRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ForSharedRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        this.fileURL = this.fileURL.toLowerCase().replace("/get/", "/file/");
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        this.fileURL = this.fileURL.toLowerCase().replace("/get/", "/file/");
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
            final String getURL = fileURL.replace("/file/", "/get/");

            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(getURL).toGetMethod();
            if (makeRedirectedRequest(httpMethod)) {

                httpMethod = getMethodBuilder().setReferer(getURL).setActionFromAHrefWhereATagContains("Click here to download this file").toGetMethod();

                final int wait = PlugUtils.getNumberBetween(getContentAsString(), "DelayTimeSec'>", "<");
                downloadTask.sleep(wait + 1);

                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new ServiceConnectionProblemException("Error starting download");
                }

            } else {
                checkProblems();
                throw new ServiceConnectionProblemException("Can't load download page");
            }

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException("Can't load download page");
        }

    }

    private void checkNameAndSize() throws Exception {
        PlugUtils.checkName(httpFile, getContentAsString(), "download ", "</title>");

        final Matcher size = getMatcherAgainstContent("Size:</b></td>\\s+?<td class=\"finforight\">([^<>]+?)</td>");
        if (!size.find()) throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size.group(1).replace(",", "")));

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }


    private void checkProblems() throws ServiceConnectionProblemException, NotRecoverableDownloadException {
        final String content = getContentAsString();
        if (content.contains("The file link that you requested is not valid")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("already downloading")) {
            throw new ServiceConnectionProblemException("Your IP address is already downloading a file");
        }
        if (content.contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException("Currently a lot of users are downloading files");
        }
        if (content.contains("You must enter a password to access this file")) {
            throw new NotRecoverableDownloadException("Files with password are not supported");
        }
    }

}