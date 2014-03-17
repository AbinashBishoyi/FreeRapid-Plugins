package cz.vity.freerapid.plugins.services.hipfile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
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
class HipFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(HipFileFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "Filename:</b></td><td nowrap>", "</td></tr>");
        PlugUtils.checkFileSize(httpFile, PlugUtils.getStringBetween(content, "Size:</b></td><td", "B") + "BX", ">", "X");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();
            checkProblems();//check problems
            final HttpMethod postMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(fileURL)
                    .setParameter("op", PlugUtils.getStringBetween(contentAsString, "name=\"op\" value=\"", "\">"))
                    .setParameter("id", PlugUtils.getStringBetween(contentAsString, "name=\"id\" value=\"", "\">"))
                    .setParameter("rand", PlugUtils.getStringBetween(contentAsString, "name=\"rand\" value=\"", "\">"))
                    .setParameter("method_free", "1")
                    .setParameter("method_premium", "")
                    .toPostMethod();
            try {
                int wait = PlugUtils.getNumberBetween(PlugUtils.getStringBetween(contentAsString, "Wait  <span id=\"", "span>"), ">", "<");
                downloadTask.sleep(wait + 1);
            } catch (final Exception e) {
                logger.info("Waiting Error");
            }

            if (!makeRequest(postMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download-1");//some unknown problem
            }
            try {
                final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains(httpFile.getFileName()).toHttpMethod();
                //here is the download link extraction
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            } catch (final Exception e) {
                throw new ServiceConnectionProblemException("Did not find download link");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("You have reached the download-limit")) {
            throw new YouHaveToWaitException("You have reached the download-limit", 3600); //let to know user in FRD
        }
        if (contentAsString.contains("No such file with this filename")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}