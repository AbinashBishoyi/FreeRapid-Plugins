package cz.vity.freerapid.plugins.services.primeshare;

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
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class PrimeShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PrimeShareFileRunner.class.getName());

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
        final Matcher matchN = PlugUtils.matcher("<h1>Watch&nbsp;\\s*?\\((.+?)\\)", content);
        if (!matchN.find()) throw new PluginImplementationException("File name not found");
        final Matcher matchS = PlugUtils.matcher(">\\((.+?)\\)<", content);
        if (!matchS.find()) throw new PluginImplementationException("File size not found");
        httpFile.setFileName(matchN.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matchS.group(1)));
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

            final int wait = PlugUtils.getNumberBetween(contentAsString, "cWaitTime =", ";");
            downloadTask.sleep(wait + 1);

            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL).setActionFromFormByName("request", true).setAction(fileURL)
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final Matcher match = PlugUtils.matcher(",\\s*?url: '(.+?)'", getContentAsString());
            if (!match.find())
                throw new PluginImplementationException("Download link not found");
            final HttpMethod dlMethod = getGetMethod(match.group(1));
            if (!tryDownloadAndSaveFile(dlMethod)) {
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
        if (contentAsString.contains("File not exist")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}