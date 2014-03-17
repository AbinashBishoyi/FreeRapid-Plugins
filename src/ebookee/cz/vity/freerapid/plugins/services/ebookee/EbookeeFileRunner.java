package cz.vity.freerapid.plugins.services.ebookee;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Abinash Bishoyi
 */
class EbookeeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(EbookeeFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
           // checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

//    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
//        PlugUtils.checkName(httpFile, content, "FileNameLEFT", "FileNameRIGHT");//TODO
//        PlugUtils.checkFileSize(httpFile, content, "FileSizeLEFT", "FileSizeRIGHT");//TODO
//        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
//    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            //final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            if (!fileURL.contains("/download/")) {
                fileURL = PlugUtils.getStringBetween(getContentAsString(), "href=\"", "\"  title");
            }
            httpFile.setNewURL(new URL(fileURL));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}