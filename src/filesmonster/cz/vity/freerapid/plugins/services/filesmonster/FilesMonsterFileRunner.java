package cz.vity.freerapid.plugins.services.filesmonster;

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
 * @author Lukiz
 */
class FilesMonsterFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilesMonsterFileRunner.class.getName());


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
        PlugUtils.checkName(httpFile, content, "File name: <span class=\"em\">", "</span>");
        PlugUtils.checkFileSize(httpFile, content, "File size: <span class=\"em\">", "</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        if (!content.contains("slowdownload")) throw new NotRecoverableDownloadException("Only Premium download available");

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
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("slowdownload", true).toHttpMethod();
                String refer = httpMethod.getPath();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            final HttpMethod ticketMethod = getMethodBuilder().setReferer(refer).setActionFromFormByName("rtForm", true).setAction("http://filesmonster.com/ajax.php").toHttpMethod();
            if (!makeRedirectedRequest(ticketMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            String data = PlugUtils.getStringBetween(getContentAsString(),"\"text\":\"", "\",\"");


            final HttpMethod getUrlMethod = getMethodBuilder().setReferer(refer).setAction("http://filesmonster.com/ajax.php")
                    .setParameter("act","getdl").setParameter("data",data).toPostMethod();
            if (!makeRedirectedRequest(getUrlMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            String finalUrl = PlugUtils.getStringBetween(getContentAsString(),"\"url\":\"", "\",\"file_request");
            String request =   PlugUtils.getStringBetween(getContentAsString(),"file_request\":\"", "\",\"err");
             finalUrl = finalUrl.replace("\\", "");

            final HttpMethod finalMethod = getMethodBuilder().setReferer(refer).setAction(finalUrl)
                    .setParameter("X-File-Request",request).toPostMethod();
            //here is the download link extraction
            if (!tryDownloadAndSaveFile(finalMethod)) {
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
        if (contentAsString.contains("File was deleted by owner")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("You can wait for the start of downloading"))
            throw new YouHaveToWaitException("You have got max allowed download sessions from the same IP", PlugUtils.getWaitTimeBetween(contentAsString, " start of downloading", " minute", TimeUnit.MINUTES));

    }

}