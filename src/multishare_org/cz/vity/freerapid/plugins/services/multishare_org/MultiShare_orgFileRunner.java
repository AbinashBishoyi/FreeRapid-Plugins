package cz.vity.freerapid.plugins.services.multishare_org;

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
 * @author birchie
 */
class MultiShare_orgFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MultiShare_orgFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "Filename:</b></td><td nowrap>", "</td></tr>");
        PlugUtils.checkFileSize(httpFile, content, "Size:</b></td><td>", " <small>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkFileProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            final HttpMethod httpMethod = getMethodBuilder()
                    .setActionFromFormByName("F1", true)
                    .setAction(fileURL)
                    .setReferer(fileURL)
                    .setParameter("method_free", "1")
                    .toPostMethod();

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")||contentAsString.contains("file was removed")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
		if (contentAsString.contains("server is in maintenance mode")) {
			throw new PluginImplementationException("This server is in maintenance mode. Please try again later.");
		}
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<h2>File Not Found</h2>")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("class=\"err\">Expired download session")) {
            throw new YouHaveToWaitException("Expired download session", 15); //let to know user in FRD
        }
        if (contentAsString.contains("class=\"err\">You have to wait")) {
            String strWaitTime = "X" + PlugUtils.getStringBetween(contentAsString, "class=\"err\">You have to wait ", " till next download") + "X";
            int intWaitTime, intWaitHour = 0, intWaitMin = 0, intWaitSec = 0;
            if (strWaitTime.contains("hour")) {
                intWaitMin = PlugUtils.getNumberBetween(strWaitTime, "X", " hour");
                strWaitTime = "X" + PlugUtils.getStringBetween(strWaitTime, ", ", "X") + "X";
            }
            if (strWaitTime.contains("minute")) {
                intWaitMin = PlugUtils.getNumberBetween(strWaitTime, "X", " minute");
                strWaitTime = "X" + PlugUtils.getStringBetween(strWaitTime, ", ", "X") + "X";
            }
            if (strWaitTime.contains("second"))
                intWaitSec = PlugUtils.getNumberBetween(strWaitTime, "X", " second");

            intWaitTime = intWaitHour * 3600 + intWaitMin * 60 + intWaitSec + 1;
            logger.info("You have to wait: " + intWaitTime + " seconds");
            throw new YouHaveToWaitException("You have to wait", intWaitTime);
        }
    }

}