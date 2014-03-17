package cz.vity.freerapid.plugins.services.ebookee;

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
class EbookeeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(EbookeeFileRunner.class.getName());

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
        if (fileURL.contains("/download/")) {
            httpFile.setNewURL(new URL(getRedirectedLink(fileURL)));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
        } else if (fileURL.contains("/download.php?id=")) {
            httpFile.setNewURL(new URL(getRedirectedLink(fileURL)));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
        } else {
            if (makeRedirectedRequest(method)) {
                checkProblems();

                final String code = PlugUtils.getStringBetween(getContentAsString(), "download/", "\"");
                final String directURL = "http://www.ebookee.ws/download/" + code;

                httpFile.setNewURL(new URL(getRedirectedLink(directURL)));
                httpFile.setPluginID("");
                httpFile.setState(DownloadState.QUEUED);
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
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

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}