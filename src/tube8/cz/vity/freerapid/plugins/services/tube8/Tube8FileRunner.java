package cz.vity.freerapid.plugins.services.tube8;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
//import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author TommyTom
 */
class Tube8FileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Tube8FileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "class=\"main-title main-sprite-img\">", "</");
        //PlugUtils.checkFileSize(httpFile, content, "class=\"btn-01 main-sprite-img relative\">[^<]?<span.*?<span class=\"video-data absolute\">(", ")</span>");
        //remove old extension if it exists
        String name = httpFile.getFileName();
        final int pos = name.lastIndexOf('.');
        if (pos != -1) name = name.substring(0, pos);
        httpFile.setFileName(name + ".flv");
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
            client.getHTTPClient().getParams().setParameter("dontUseHeaderFilename", true);
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromTextBetween("videourl=\"", "\"").toGetMethod();
            // alternate:
            // final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromTextBetween("flashvars.video_url = '", "'").toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        //TODO detect redirect to index as there is no error
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}