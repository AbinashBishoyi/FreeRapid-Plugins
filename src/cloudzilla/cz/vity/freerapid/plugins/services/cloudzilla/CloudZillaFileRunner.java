package cz.vity.freerapid.plugins.services.cloudzilla;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
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
 * @author birchie
 */
class CloudZillaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CloudZillaFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "class=\"name\" title=\"", "\">");
        PlugUtils.checkFileSize(httpFile, content, "class=\"size\">(", ")");
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
            final String fId = PlugUtils.getStringBetween(contentAsString, "freeDownload('", "'");
            final HttpMethod infoMethod = getMethodBuilder().setReferer(fileURL)
                    .setAction("/generateticket/").setAjax()
                    .setParameter("file_id", fId)
                    .setParameter("key", "")
                    .toPostMethod();
            if (!makeRedirectedRequest(infoMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            if (!getContentAsString().contains("<status>ok"))
                throw new PluginImplementationException("Error getting download link");
            final String server = PlugUtils.getStringBetween(getContentAsString(), "<server>", "</server>");
            final String ticket = PlugUtils.getStringBetween(getContentAsString(), "<ticket_id>", "</ticket_id>");
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL)
                    .setAction("http://" + server + "/download/" + fId + "/" + ticket)
                    .toGetMethod();
            downloadTask.sleep(1 + PlugUtils.getNumberBetween(getContentAsString(), "<wait>", "</wait>"));
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
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