package cz.vity.freerapid.plugins.services.lnx_lu;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class Lnx_LuFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Lnx_LuFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems

            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("images/skipadbtn.gif")
                    .toHttpMethod();
            final int httpStatus = client.makeRequest(httpMethod, false);
            if (httpStatus / 100 == 3) {
                final Header locationHeader = httpMethod.getResponseHeader("Location");
                if (locationHeader == null) {
                    throw new PluginImplementationException("Invalid redirect");
                }
                this.httpFile.setNewURL(new URL(locationHeader.getValue())); //to setup new URL
                this.httpFile.setFileState(FileState.NOT_CHECKED);
                this.httpFile.setPluginID(""); //to run detection what plugin should be used for new URL, when file is in QUEUED state
                this.httpFile.setState(DownloadState.QUEUED);
            } else {
                throw new PluginImplementationException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The URL you have provided does not seem to be valid")) {
            throw new URLNotAvailableAnymoreException("Invalid link"); //let to know user in FRD
        }
    }

}