package cz.vity.freerapid.plugins.services.flyshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek
 */
class FlyshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FlyshareRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        checkProblems();        //has to be
        Matcher matcher = PlugUtils.matcher("class=\"filename\">(.*?)<", content);
        if (matcher.find()) {
            final String fileName = matcher.group(1).trim(); //method trim removes white characters from both sides of string
            httpFile.setFileName(fileName);
            //<span class="filename">nprtprdbrnm_wassup.part7.rar</span> (96.872 MB)</p>
            matcher = PlugUtils.matcher("</span> \\(([0-9. ]+.?B)\\)<", content);
            if (matcher.find()) {
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
                httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
            } else {
                checkProblems();
                logger.warning("File size was not found\n:");
                //throw new PluginImplementationException();
            }
        } else {
            checkProblems();
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }
    }


    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkNameAndSize(contentAsString);
            //client.setReferer(fileURL);
            final Matcher matcher = getMatcherAgainstContent("href=\"(http.+?)\\?view_login");
            if (matcher.find()) {
                final String newURL = matcher.group(1);
                logger.info("Parsed new URL" + newURL);
                client.setReferer(newURL);
                final PostMethod postMethod = getPostMethod(newURL);
                postMethod.addParameter("method", "free");
                postMethod.addParameter("wmod_command", "wmod_fileshare3:startDownload");

                if (!tryDownloadAndSaveFile(postMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty.");
                }
            } else throw new PluginImplementationException("Cannot find downloading URL");
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Invalid request") || contentAsString.contains("File is not available on the server")) {
            throw new URLNotAvailableAnymoreException("File is not available on the server");
        }

        if (contentAsString.contains("error: Too many simultaneous")) {
            throw new ServiceConnectionProblemException("Too many simultaneous downloads, try again later.");
        }
//
//        if (contentAsString.contains("Neplatny download")) {
//            throw new YouHaveToWaitException("Neplatny download", 2);
//        }
    }
}
