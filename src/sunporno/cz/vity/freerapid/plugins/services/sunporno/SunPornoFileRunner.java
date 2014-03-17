package cz.vity.freerapid.plugins.services.sunporno;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
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
class SunPornoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SunPornoFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "<title>", "</title>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
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

            HttpMethod httpMethod = getGetMethod(PlugUtils.getStringBetween(contentAsString, "<a href=\"", "\">Download movie"));
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error getting download link");
            }
            httpMethod = getGetMethod(PlugUtils.getStringBetween(getContentAsString(), "href=\"", "\"> Click here to download"));

            Matcher match = PlugUtils.matcher("href=\".+(\\.\\w{3})\"> Click here to download", getContentAsString());
            if (!match.find()) {
                throw new ErrorDuringDownloadingException("Error getting file type");
            }
            httpFile.setFileName(httpFile.getFileName() + match.group(1));

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The file you have requested was not found on this server")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}