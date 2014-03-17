package cz.vity.freerapid.plugins.services.fodashare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FodaShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FodaShareFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            httpFile.setFileName(fileURL.substring(1 + fileURL.lastIndexOf("/")));
            final HttpMethod adMethod = getMethodBuilder()
                    .setActionFromAHrefWhereATagContains("download.jpg")
                    .toGetMethod();
            if (!makeRedirectedRequest(adMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final HttpMethod httpMethod = getMethodBuilder()
                    .setActionFromAHrefWhereATagContains("DOWNLOAD")
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download - Check URL");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("This website is offline") ||
                contentAsString.contains("Website is currently unreachable") ||
                contentAsString.contains("The website that you are trying to access is in Offline Mode")) {
            throw new ErrorDuringDownloadingException("Website error (try again) OR File does not exist");
        }
    }

}