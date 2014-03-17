package cz.vity.freerapid.plugins.services.filesonic;

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
 * @author JPEXS
 */
class FileSonicFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileSonicFileRunner.class.getName());


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
        PlugUtils.checkName(httpFile, content, "<span>Filename: </span> <strong>", "</strong>");
        PlugUtils.checkFileSize(httpFile, content, "<span class=\"size\">", "</span>");
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
            String freeUrl=PlugUtils.getStringBetween(getContentAsString(),"<a href=\"","\" id=\"free_download\">");

            final HttpMethod methodFree=getMethodBuilder().setReferer(fileURL).setAction(freeUrl).toGetMethod();
            if(!makeRedirectedRequest(methodFree)){
                throw new PluginImplementationException();
            }
            final String content=getContentAsString();
            if(content.contains("Download session in progress")){
                 throw new YouHaveToWaitException("Download session in progress",30);
            }
            final String downloadUrl=PlugUtils.getStringBetween(content,"var downloadUrl = \"","\";");
            final int waitTime=PlugUtils.getWaitTimeBetween(content,"var countDownDelay = ",";", TimeUnit.SECONDS);
            downloadTask.sleep(waitTime);
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(downloadUrl).toGetMethod();

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
        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}