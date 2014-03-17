package cz.vity.freerapid.plugins.services.imgbox;

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
 * @author birchie
 */
class imgBoxFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(imgBoxFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        if (!fileURL.contains("i.imgbox")) {
            final GetMethod getMethod = getGetMethod(fileURL);//make first request
            if (makeRedirectedRequest(getMethod)) {
                checkProblems();
                checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        if (!fileURL.contains("i.imgbox"))
            PlugUtils.checkName(httpFile, content, "title=\"", "\"");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL); //create GET request
        if (!fileURL.contains("i.imgbox")) {
            if (!makeRedirectedRequest(method)) { //we make the main request
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            Matcher match = PlugUtils.matcher("<img.*src=\"(.+)\" title", getContentAsString());
            if (!match.find())
                throw new PluginImplementationException("Unable to find image");
            method = getMethodBuilder().setAction(match.group(1)).setReferer(fileURL).toGetMethod();
        }
        //here is the download link extraction
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();//if downloading failed
            throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The image in question does not exist") ||
                contentAsString.contains("The file you are looking for could not be found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}