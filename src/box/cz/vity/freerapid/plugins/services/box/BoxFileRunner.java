package cz.vity.freerapid.plugins.services.box;

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
 * @author birchie
 */
class BoxFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BoxFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        int httpStatus = client.makeRequest(getMethod, false);
        if (httpStatus / 100 == 3) {
        } else if (httpStatus == 200) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<title>", " - File");
        PlugUtils.checkFileSize(httpFile, content, "<span>(", ")</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        int httpStatus = client.makeRequest(method, false);
        if (httpStatus / 100 == 3) {
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
            return;
        } else if (httpStatus != 200) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        final String contentAsString = getContentAsString();//check for response
        checkProblems();//check problems
        checkNameAndSize(contentAsString);
        final String sName = PlugUtils.getStringBetween(contentAsString, "var shared_name = '", "';");
        final String fId = PlugUtils.getStringBetween(contentAsString, "var file_id = '", "';");
        final Matcher match = PlugUtils.matcher("<a href=\"(.+?" + sName + ")\"", contentAsString);
        if (!match.find())
            throw new PluginImplementationException("Download link not found");
        final HttpMethod httpMethod = getGetMethod(match.group(1).replace("file_id=f_", "file_id=f_" + fId).replace("&amp;", "&"));
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();//if downloading failed
            throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("error_message_not_found") ||
                contentAsString.contains("This shared file or folder link has been removed")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("The user hosting this content is out of bandwidth")) {
            throw new NotRecoverableDownloadException("The user hosting this content is out of bandwidth");
        }
    }

}