package cz.vity.freerapid.plugins.services.therapide;

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
class TheRapideFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TheRapideFileRunner.class.getName());

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
        final Matcher matchName = PlugUtils.matcher("\"file-dash-title\"[^>]*?>\\s*?(.+?)\\s*?</div>", content);
        final Matcher matchSize = PlugUtils.matcher("\"file-detail-size\"[^>]*?>\\s*?(.+?)\\s*?</div>", content);
        if (!matchName.find()) throw new PluginImplementationException("File name not found");
        if (!matchSize.find()) throw new PluginImplementationException("File size not found");
        httpFile.setFileName(matchName.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matchSize.group(1)));
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

            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL).setAjax()
                    .setAction("/en/files/downloadasync")
                    .setParameter("id", PlugUtils.getStringBetween(contentAsString, "id: '", "'"))
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                throw new ServiceConnectionProblemException();
            }
            checkErrors();
            final String dlUrl = PlugUtils.getStringBetween(getContentAsString(), "\"downloadUrl\":\"", "\"");
            logger.info("###### " + dlUrl);
            if (!tryDownloadAndSaveFile(getGetMethod(dlUrl))) {
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
        if (contentAsString.contains("<title>TheRapide.com")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private void checkErrors() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("\"errorType\":\"CountExceeded\"")) {
            throw new ServiceConnectionProblemException("Another download is in progress");
        }
        if (content.contains("\"errorType\":\"SameFileDownload\"")) {
            throw new ServiceConnectionProblemException("You are already downloading the file");
        }
        if (content.contains("\"errorType\":\"FileIsNotAvailable\"")) {
            throw new ServiceConnectionProblemException("The file is not currently available");
        }
    }

}