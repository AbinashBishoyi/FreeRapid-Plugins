package cz.vity.freerapid.plugins.services.xhamster;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
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
class xHamsterFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(xHamsterFileRunner.class.getName());

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
        httpFile.setFileName(PlugUtils.getStringBetween(PlugUtils.getStringBetween(content, "element_str_id", "/div>"), ">", "<") + ".flv");
        PlugUtils.checkFileSize(httpFile, content, "Download this video (", ")");
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

            String Re_URL = PlugUtils.getStringBetween(PlugUtils.getStringBetween(contentAsString, "id=\"embed_code_source\"", "</textarea>"), "src=\"", "\"");
            HttpMethod methodB = getGetMethod(Re_URL);
            if (!makeRedirectedRequest(methodB)) {
                checkProblems();    //if redirecting failed
                throw new ServiceConnectionProblemException("Error starting download-1");//some unknown problem
            }

            final String contentAsStringB = getContentAsString();//check for response
            String flvServer = PlugUtils.getStringBetween(contentAsStringB, "srv=", "&");
            String flvJoiner;
            if (contentAsStringB.contains("url_mode")) flvJoiner = "/key=";
            else flvJoiner = "/flv2/";
            String flvDetail = PlugUtils.getStringBetween(contentAsStringB, "file=", "&");
            String FullFlvHttp = flvServer + flvJoiner + flvDetail;

            HttpMethod methodC = getMethodBuilder().setAction(FullFlvHttp).toGetMethod();

            if (!tryDownloadAndSaveFile(methodC)) {
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
        if (contentAsString.contains("not found on this server")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }
}
