package cz.vity.freerapid.plugins.services.videoweed;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author TommyTom, ntoskrnl
 */
class VideoWeedFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(VideoWeedFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("/file/(.+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        httpFile.setFileName(matcher.group(1) + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            checkNameAndSize();//extract file name and size from the page
            //we don't want crap in the filename
            client.getHTTPClient().getParams().setParameter("dontUseHeaderFilename", true);
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromTextBetween(".file=\"", "\";")
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
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
        if (content.contains("This file no longer exists on our servers") || content.contains("<h1>404 - Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let user know in FRD
        }
    }

}