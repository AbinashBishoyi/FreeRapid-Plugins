package cz.vity.freerapid.plugins.services.bigandfree;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class BigAndFreeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BigAndFreeFileRunner.class.getName());


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
        PlugUtils.checkName(httpFile, content, "File Name: </font><font class=\"type3\">", "</font>");
        //PlugUtils.checkFileSize(httpFile, content, "FillSizeLEFT", "FileNameRIGHT");
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
            if (makeRedirectedRequest(method)) { //we make the main request
                checkProblems();//check problems
                final String formContent = PlugUtils.getStringBetween(getContentAsString(), "document.getElementById(\"download\").innerHTML = '", "'");
                final HttpMethod httpMethod = getMethodBuilder("<form method=\"POST\" name=\"download_now\">" + formContent + "</form>  ").setActionFromFormByIndex(1, true).setAction(fileURL).removeParameter("direct_now").toPostMethod();
                //waiting isn't necessary
                //final int sleep = PlugUtils.getNumberBetween(getContentAsString(), "var x = ", ";");
                //downloadTask.sleep(sleep);
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    logger.warning(getContentAsString());//log the info
                    throw new PluginImplementationException();//some unknown problem
                }
            } else throw new PluginImplementationException();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<font class=\"type3\">N/A</font>") || contentAsString.contains("requested has been removed")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Please wait")) {
            try {
                final int timeBetween = PlugUtils.getWaitTimeBetween(contentAsString, "Please wait", "Minute", TimeUnit.MINUTES);
                throw new YouHaveToWaitException("You have exceeded your download limit. Please wait.", timeBetween); //let to know user in FRD
            } catch (PluginImplementationException e) {
                throw new YouHaveToWaitException("You have exceeded your download limit. Please wait.", 60); //let to know user in FRD
            }
        }
    }

}