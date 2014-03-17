package cz.vity.freerapid.plugins.services.ebooksshare;

import cz.vity.freerapid.plugins.exceptions.BuildMethodException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Abinash Bishoyi
 */
class EbooksshareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(EbooksshareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (fileURL.contains("/redirect/")) {
            httpFile.setNewURL(new URL(getRedirectedLink(fileURL)));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
        } else {
            if (makeRedirectedRequest(method)) {
                checkProblems();

                final String id = PlugUtils.getStringBetween(getContentAsString(), "span id=\"", "\">");
                logger.info("ID: " + id);
                final String directURL = "http://www.ebooks-share.net/redirect/" + id;

                httpFile.setNewURL(new URL(getRedirectedLink(directURL)));
                httpFile.setPluginID("");
                httpFile.setState(DownloadState.QUEUED);
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("To continue use of our service")) {
            throw new ServiceConnectionProblemException("You need to signup to use the service");
        }
    }

    private String getRedirectedLink(String link) throws BuildMethodException, IOException, ServiceConnectionProblemException {
        final MethodBuilder methodBuilder = getMethodBuilder().setBaseURL(link);
        final HttpMethod get = methodBuilder.toHttpMethod();
        int code = client.makeRequest(get, false);

        logger.info("Response: " + code);

        if (code != HttpStatus.SC_MOVED_TEMPORARILY) {
            throw new ServiceConnectionProblemException("Error following link");
        }

        final Header hLocation = get.getResponseHeader("Location");

        if (hLocation == null) {
            throw new ServiceConnectionProblemException("Error following link");
        }

        final String location = hLocation.getValue();
        logger.info("Location: " + location);
        return location;
    }

    private void checkFileURL() {
        //Need a good proxy service
        fileURL = "http://www.zend2.com/bb.php?u=" + fileURL + "&b=24&f=norefer";//Use the proxy to avoid the signup process
    }
}