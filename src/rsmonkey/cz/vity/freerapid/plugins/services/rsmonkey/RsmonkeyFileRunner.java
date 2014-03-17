package cz.vity.freerapid.plugins.services.rsmonkey;

//import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
//import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
//import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @author Arthur Gunawan
 */
class RsmonkeyFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RsmonkeyFileRunner.class.getName());


    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            try {
                //Find forwarded URL in the content
                String s = PlugUtils.getStringBetween(contentAsString, "frameborder=\"no\" width=\"100%\" src=\"", "\"></iframe>");
                final String escapedURI = getMethodBuilder().setAction(s).toHttpMethod().getURI().getEscapedURI();
                logger.info("New Link : " + escapedURI);     //Debug purpose, show the new found link
                this.httpFile.setNewURL(new URL(escapedURI));  //Set New URL for the link
            } catch (MalformedURLException e) {        //Catch error url
                throw new URLNotAvailableAnymoreException("Invalid URL");
            }
            this.httpFile.setPluginID("");
            this.httpFile.setState(DownloadState.QUEUED);

        } else {
            checkProblems();
        }
    }

    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("The requested document was not found on this server")) {
            throw new ServiceConnectionProblemException("RSMonkey Error : Link was not found on this server!");
        }

    }


}
