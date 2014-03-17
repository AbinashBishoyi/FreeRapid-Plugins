package cz.vity.freerapid.plugins.services.ololo;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class OloloFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(OloloFileRunner.class.getName());

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

    private void checkNameAndSize(String content) throws Exception {
        httpFile.setFileName(URLDecoder.decode(PlugUtils.getStringBetween(content, "/mp3/", "\""), "UTF-8"));
        //PlugUtils.checkFileSize(httpFile, content, "FileSizeLEFT", "FileSizeRIGHT");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            checkNameAndSize(getContentAsString());//extract file name and size from the page
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("MP3").toGetMethod();
            client.getHTTPClient().getParams().setBooleanParameter("dontUseHeaderFilename", true);
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
        final String content = getContentAsString();
        if (content.contains("а!</h2><p>")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (content.contains("Service Temporarily Unavailable")) {
            throw new ErrorDuringDownloadingException("Download error - already downloading ?");
        }
    }

}