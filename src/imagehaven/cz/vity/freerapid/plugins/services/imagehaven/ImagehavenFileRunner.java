package cz.vity.freerapid.plugins.services.imagehaven;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Arthur Gunawan,JPEXS
 */
class ImagehavenFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ImagehavenFileRunner.class.getName());


    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request

        final String SERVER = PlugUtils.getStringBetween(fileURL,"","/img.php");
        final String fName = PlugUtils.getStringBetween(fileURL + "\"","id=","\"");
        httpFile.setFileName(fName);
        
        logger.info("Server : " + SERVER);


        if (makeRedirectedRequest(method)) { //we make the main request
            String contentAsString = getContentAsString();//check for response
            if(contentAsString.contains("This ad is shown once a day")){ //There is ad on adult images
              if(!makeRedirectedRequest(method)){ //We have to reload page (Cookies was already stored)
                checkProblems();
                throw new ServiceConnectionProblemException();
              }
              contentAsString = getContentAsString();
            }
             
            checkProblems();//check problems
        
            String mLink = PlugUtils.getStringBetween(contentAsString,"<img src='.","'");
            mLink = SERVER + mLink;
            final String escapedURI = getMethodBuilder().setAction(mLink).toHttpMethod().getURI().getEscapedURI();
            logger.info("Link : " + mLink);
            final HttpMethod httpMethod = getMethodBuilder().setReferer(escapedURI).setAction(escapedURI).toHttpMethod();//TODO

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}
