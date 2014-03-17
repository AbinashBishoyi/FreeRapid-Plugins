package cz.vity.freerapid.plugins.services.spankwire;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Random;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author User
 */
class SpankWireFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SpankWireFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "<h1>", "</h1>");
//        PlugUtils.checkFileSize(httpFile, content, "FileSizeLEFT", "FileSizeRIGHT");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        httpFile.setFileName(httpFile.getFileName() + ".flv");
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            final int i = fileURL.lastIndexOf("video");
            String id = fileURL.substring(i + 5).replaceAll("/", "");
            logger.info("The video is is " + id);
            String url = String.format("http://cdn1.static.spankwire.com/Controls/UserControls/Players/v3/PlaylistXml.aspx?id=%s&pid=2&start=0&r=0.500769003527597&noCache=%s", id, new Random().nextInt(1000));
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(url).toHttpMethod();
            if (makeRedirectedRequest(httpMethod)) {
                url = PlugUtils.getStringBetween(getContentAsString(), "<url>", "</url>");
                httpMethod = getMethodBuilder().setReferer(fileURL).setAction(url).toHttpMethod();
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            } else throw new PluginImplementationException("Video link was not found");
            //here is the download link extraction
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Error Page Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("This article is temporarily unavailable.")) {
            throw new YouHaveToWaitException("This article is temporarily unavailable. Please try again in a few minutes.", 5);
        }
    }

}