package cz.vity.freerapid.plugins.services.kewlshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Arthur Gunawan
 */
class KewlshareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(KewlshareFileRunner.class.getName());
    public boolean isFinal;


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            throw new PluginImplementationException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {

            checkProblems();


        if (!content.contains("File Name")) {
            checkProblems();
            logger.warning(getContentAsString());
            throw new ServiceConnectionProblemException("Kewlshare Server Error");
        }

        PlugUtils.checkName(httpFile, content, "File Name : <strong>", " || ");//TODO
        PlugUtils.checkFileSize(httpFile, content, " || ", "</strong>");//TODO
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response

            checkNameAndSize(contentAsString);//extract file name and size from the page

            isFinal = false;

            while (!isFinal) {
                ProccessHTML(getContentAsString());
                logger.info(getContentAsString());
                
            }

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }


    private void ProccessHTML(String content) throws Exception {
        if (content.contains("http://kewlshare.com/button/free.gif")) {
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setAction(fileURL).setActionFromFormWhereTagContains("http://kewlshare.com/button/free.gif", true).toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Can't post first page");//some unknown problem
            }
            logger.info("HTML Process 1 OK!");


        }

        if (content.contains("name=\"selection\" value=\"Free\" type=\"hidden\"")) {
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setAction(fileURL).setActionFromFormWhereTagContains("input id=\"imageInput\"", true).toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Can't post first page");//some unknown problem
            }
            logger.info("HTML Process 2 OK!");


        }


        if (content.contains("http://kewlshare.com/img/pod.gif")) {
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setAction(fileURL).setActionFromFormWhereTagContains("http://kewlshare.com/img/pod.gif", true).toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Can't post first page");//some unknown problem
            }
            logger.info("HTML Process 3 OK!");


        }
        if (content.contains("http://kewlshare.com/img/down.gif")) {
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setActionFromFormWhereTagContains("http://kewlshare.com/img/down.gif", true).toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Can't download");//some unknown problem
            }

            logger.info("HTML Process 4 OK!");


        }
        if (content.contains("Click Here If your Download Doesn't Start Automatically")) {
            String newURL = PlugUtils.getStringBetween(getContentAsString(), "<a href=\"", "\"> <span class=\"stylet\">");
            logger.info("Final URL: " + newURL);

            GetMethod method = getGetMethod(newURL);
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Can't download");//some unknown problem
            }

            logger.info("HTML Process 5 OK!");

            isFinal = true;


        }

            checkProblems();


    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Download Limit")) {//TODO
            throw new YouHaveToWaitException("Download Limit 1 hour is over", 3600); //let to know user in FRD
        }
        if (contentAsString.contains("Please Inform us if you see this Error")) {//TODO
            throw new ServiceConnectionProblemException("Kewlshare server error"); //let to know user in FRD
        }
        if (contentAsString.contains("This Server Usage is really high in this moment")) {//TODO
            throw new ServiceConnectionProblemException("Kewlshare server error"); //let to know user in FRD
        }
    }

}
