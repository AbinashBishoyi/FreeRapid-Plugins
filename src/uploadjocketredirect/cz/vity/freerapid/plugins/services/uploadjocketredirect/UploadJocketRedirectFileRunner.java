package cz.vity.freerapid.plugins.services.uploadjocketredirect;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Class which contains main code
 *
 * @author Arthur Gunawan
 */
class UploadJocketRedirectFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadJocketRedirectFileRunner.class.getName());


    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(fileURL).toHttpMethod(); //create GET request

        if (!makeRedirectedRequest(method)) { //we make the main request
            checkProblems();
            throw new ServiceConnectionProblemException();

        }

        final String contentAsString = getContentAsString();//check for response

        final String s = PlugUtils.getStringBetween(contentAsString, "<iframe src=\"", "\"");

        try {
            this.httpFile.setNewURL(new URL(s));
        } catch (MalformedURLException e) {
            throw new URLNotAvailableAnymoreException("Invalid URL");
        }
        this.httpFile.setPluginID("");
        this.httpFile.setState(DownloadState.QUEUED);


    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}