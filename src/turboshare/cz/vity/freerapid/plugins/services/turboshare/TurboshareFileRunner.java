package cz.vity.freerapid.plugins.services.turboshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
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
 * @author Arthur Gunawan
 */
class TurboshareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TurboshareFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h2>Download File ", "</h2>");//TODO
        PlugUtils.checkFileSize(httpFile, content, "</font> (", ")</font>");//TODO
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

            //Post First Form

            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL).setBaseURL(fileURL).setActionFromFormByIndex(1, true).setParameter("method_free", "Free Download").removeParameter("method_premium").toHttpMethod();//TODO

            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
            }
            logger.info("Phase 1 OK!");//log the info

            final int waitTime = PlugUtils.getWaitTimeBetween(getContentAsString(), "<span id=\"countdown\">", "</span>", TimeUnit.SECONDS);
            downloadTask.sleep(waitTime);
            httpMethod = getMethodBuilder().setActionFromFormByName("F1", true).setReferer(fileURL).setAction(fileURL).setParameter("method_free", "Free Download").removeParameter("method_premium").toPostMethod();

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
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}
