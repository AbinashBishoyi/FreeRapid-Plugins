package cz.vity.freerapid.plugins.services.filesflash;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Heend
 */
class FilesFlashRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilesFlashRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }


    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<td style=\"text-align:right\">Filename:", "<br />");
        PlugUtils.checkFileSize(httpFile, content, "Size:", "</td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL); //create GET request

        if (makeRedirectedRequest(method)) { //we make the main request
            final String content = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(content);//extract file name and size from the page

            //here is the download link extraction
            method = getMethodBuilder().setReferer(fileURL).setActionFromFormWhereTagContains("freedownload.php", true).toPostMethod();

            if (makeRedirectedRequest(method)) {
                if (getContentAsString().contains("Please Wait")) {
                    final int waitTime = PlugUtils.getWaitTimeBetween(getContentAsString(), "count=", ";", TimeUnit.SECONDS);
                    downloadTask.sleep(waitTime);

                    if (getContentAsString().contains("Click here to start free download")) {
                        method = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Click here to start free download").toPostMethod();
                    }

                }

                if (getContentAsString().contains("Your IP address is already downloading another link.")) {
                    throw new ServiceConnectionProblemException("Free users may only download one file at a time.");
                }

                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
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
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }

    }

}