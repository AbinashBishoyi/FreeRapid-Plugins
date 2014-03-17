package cz.vity.freerapid.plugins.services.pimpandhost;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class PimpAndHostFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PimpAndHostFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        //Matcher matcher = PlugUtils.matcher("-original\\.html$",fileURL);
        //String iUrl;

        String nextURL;
        //String contentAsString = getContentAsString();
        if (!PlugUtils.find("-original\\.html$", fileURL)) {
            if (PlugUtils.find("\\.html$",fileURL)) {
                nextURL = fileURL.replaceFirst("-(small|medium)\\.html$","-original.html");
            } else {
                nextURL = fileURL.replaceFirst("/show/id/(\\d+)$","/$1-original.html");
            }
        } else {
            nextURL = fileURL;
        }
        logger.info("Starting download in TASK " + nextURL);
        final GetMethod method = getGetMethod(nextURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            //checkNameAndSize(contentAsString);//extract file name and size from the page
            
            String imgUrl = PlugUtils.getStringBetween(contentAsString, "id=\"image\" src=\"", "\"/");
            logger.info(imgUrl);
            Matcher matcher = PlugUtils.matcher("/([^/]+\\.\\w+$)", imgUrl);
            if (!matcher.find()) {
                logger.warning("File not found");
                throw new URLNotAvailableAnymoreException("File not found");
            }
            httpFile.setFileName(matcher.group(1));
            final HttpMethod httpMethod = getMethodBuilder().setReferer(nextURL).setAction(imgUrl).toHttpMethod();//TODO

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
        if (contentAsString.contains("Image was removed")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}