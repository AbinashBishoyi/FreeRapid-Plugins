package cz.vity.freerapid.plugins.services.sourceforge;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Abinash Bishoyi
 */
class SourceForgeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SourceForgeFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final URI downloadLinkUri;
        final String filename;
        try {
            downloadLinkUri = new URI(getDownloadLinkMethodBuilder().getEscapedURI());
            filename = URLDecoder.decode(downloadLinkUri.getPath().substring(downloadLinkUri.getPath().lastIndexOf("/") + 1), "UTF-8");
        } catch (Exception e) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(filename);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            checkNameAndSize();//extract file name and size from the page
            final HttpMethod httpMethod = getDownloadLinkMethodBuilder().toHttpMethod();

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

    private MethodBuilder getDownloadLinkMethodBuilder() throws BuildMethodException {
        return getMethodBuilder().setReferer(fileURL).setActionFromTextBetween("url=", "\">");
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}