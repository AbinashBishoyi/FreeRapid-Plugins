package cz.vity.freerapid.plugins.services.instagram;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class InstagramFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(InstagramFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getDownloadLink(getContentAsString()));//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String downloadLink) throws ErrorDuringDownloadingException {
        String filename;
        try {
            filename = PlugUtils.suggestFilename(downloadLink);
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("File name not found");
        }
        logger.info("File name: " + filename);
        httpFile.setFileName(filename);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            String downloadLink = getDownloadLink(getContentAsString());
            checkNameAndSize(downloadLink);
            if (!tryDownloadAndSaveFile(getGetMethod(downloadLink))) {
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
        if (contentAsString.contains("Page Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private String getDownloadLink(String content) throws PluginImplementationException {
        if (content.contains("\"video_url\"")) {
            return PlugUtils.getStringBetween(content, "\"video_url\":\"", "\"").replace("\\/", "/");
        } else if (content.contains("\"display_src\"")) {
            return PlugUtils.getStringBetween(content, "\"display_src\":\"", "\"").replace("\\/", "/");
        } else {
            throw new PluginImplementationException("Download link not found");
        }
    }

}