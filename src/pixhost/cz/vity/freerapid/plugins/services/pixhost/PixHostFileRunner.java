package cz.vity.freerapid.plugins.services.pixhost;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class PixHostFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PixHostFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        checkUrl();
        if (!fileURL.contains("pixhost.org/images/")) {
            final GetMethod getMethod = getGetMethod(fileURL);//make first request
            if (makeRedirectedRequest(getMethod)) {
                checkProblems();
                checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else
            checkNameAndSize(fileURL);
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        if (fileURL.equals(content))
            httpFile.setFileName(content.substring(1 + content.lastIndexOf("/")));
        else
            PlugUtils.checkName(httpFile, content, "h2>Picture: ", "</h2>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkUrl() throws PluginImplementationException {
        if (fileURL.contains("pixhost.org/thumbs/")) {
            fileURL = "http://www.pixhost.org/show/" + fileURL.split("pixhost.org/thumbs/")[1];
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkUrl();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL); //create GET request

        if (!fileURL.contains("pixhost.org/images/")) {
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);
            method = getGetMethod(PlugUtils.getStringBetween(contentAsString, "<img id=\"show_image\" src=\"", "\" onClick"));
        } else
            checkNameAndSize(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();//if downloading failed
            throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Picture doesn't exist")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}