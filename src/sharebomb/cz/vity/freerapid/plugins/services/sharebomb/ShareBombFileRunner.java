package cz.vity.freerapid.plugins.services.sharebomb;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Eterad
 */
class ShareBombFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShareBombFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<strong>", "</strong>");
        PlugUtils.checkFileSize(httpFile, content, "<strong>Size:</strong> ", "</li>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            String URL = PlugUtils.getStringBetween(contentAsString, "dlLink=unescape('", "');");
            URL = URL.replace("%3A", ":");
            URL = URL.replace("%2F", "/");
            logger.info(">>>>> URL: "+ URL + " <<<<<");
            HttpMethod gMethod = getMethodBuilder().setAction(URL).toGetMethod();
            client.setReferer(fileURL);
            final int resultCode = client.makeRequest(gMethod, false);
            if(!isRedirect(resultCode))
                throw new PluginImplementationException("Redirect not found");
            final Header responseLocation = gMethod.getResponseHeader("Location");
            if(responseLocation == null)
                throw new PluginImplementationException("Location header not found");
            logger.info("\n---------------------\n"+responseLocation.getValue()+"\n---------------------");
            final String finalURL = responseLocation.getValue();
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(finalURL).toHttpMethod();
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
        if (contentAsString.contains("Select the files you")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        } else if(contentAsString.contains("This file has been deleted")){
            throw new URLNotAvailableAnymoreException("File has been deleted");
        }
    }

    protected boolean isRedirect(int resultCode){
        return (resultCode == HttpStatus.SC_MOVED_TEMPORARILY) || (resultCode == HttpStatus.SC_MOVED_PERMANENTLY) ||
                (resultCode == HttpStatus.SC_SEE_OTHER) || (resultCode == HttpStatus.SC_TEMPORARY_REDIRECT);
    }

}