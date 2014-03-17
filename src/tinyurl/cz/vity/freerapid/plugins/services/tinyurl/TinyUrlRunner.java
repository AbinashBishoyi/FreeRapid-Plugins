package cz.vity.freerapid.plugins.services.tinyurl;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;



import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.HttpStatus;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Alex
 */
class TinyUrlRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TinyUrlRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting run task " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        logger.info(fileURL);

       
 
        //it works to me .... compilable
            if (client.makeRequest(method, false) == HttpStatus.SC_MOVED_PERMANENTLY) {

         
                String s = method.getResponseHeaders("Location").toString();
                logger.info(s);
                this.httpFile.setNewURL(new URL(s));
                this.httpFile.setPluginID("");
                this.httpFile.setState(DownloadState.QUEUED);

          
            } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
            }
        } 

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("was not found")) {
            throw new URLNotAvailableAnymoreException("The page you requested was not found in our database.");
        }
    }

}
