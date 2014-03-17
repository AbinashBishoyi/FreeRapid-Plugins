package cz.vity.freerapid.plugins.services.streamupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class StreamUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(StreamUploadFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "Filename :", "<br");
        PlugUtils.checkFileSize(httpFile, content, "Filesize :", "<br");
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

            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL).setAction(fileURL)
                    .setParameter("submit", "download")
                    .toPostMethod();
            final int status = client.makeRequest(httpMethod, false);
            if (status / 100 == 3) {
                String url = httpMethod.getResponseHeader("Location").getValue();
                if (url.lastIndexOf("http://") > 0)
                    url = url.substring(url.lastIndexOf("http://"));
                httpMethod = getGetMethod(url);
            }
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
        if (contentAsString.contains("The requested video could not be found") ||
                contentAsString.contains("the link is incorrect") ||
                contentAsString.contains("the file never existed") ||
                contentAsString.contains("the file was deleted")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("You can only download one file at a time"))
            throw new ErrorDuringDownloadingException("You can only download one file at a time");

    }

}