package cz.vity.freerapid.plugins.services.storage;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class StorageFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(StorageFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "Downloading:</span>", "<span class=\"light\"");
        PlugUtils.checkFileSize(httpFile, content, "class=\"light\">(", ")</span>");
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
            final String directLink = PlugUtils.getStringBetween(getContentAsString(), "you can visit ", " directly");
            String state;
            ResponseParser json;
            do {
                json = getParser(directLink);
                state = json.getString("state");
                if ("wait".equals(state)) {
                    final int countDown = json.getInt("countdown");
                    downloadTask.sleep(countDown + 1);
                } else break;
            } while (true);
            if ("failed".equals(state)) {
                throw new ServiceConnectionProblemException("The download failed. Please try again later");
            } else if ("ok".equals(state)) {
                final String link = json.getString("link");
                logger.info("link:" + link);
                final int countDown = json.getInt("countdown");
                downloadTask.sleep(countDown + 1);//musi byt
                //here is the download link extraction
                //    this.client.getHTTPClient().getParams().setParameter("dontUseHeaderFilename", true);
                if (!tryDownloadAndSaveFile(getMethodBuilder().setReferer(fileURL).setAction(link).toHttpMethod())) {
                    checkProblems();//if downloading failed
                    logger.warning(getContentAsString());//log the info
                    throw new PluginImplementationException();//some unknown problem
                }
            } else throw new PluginImplementationException("Unknown state");//some unknown problem
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private ResponseParser getParser(String directLink) throws PluginImplementationException, IOException {
        final MethodBuilder mb = getMethodBuilder().setReferer(fileURL).setAction(directLink);
        if (makeRedirectedRequest(mb.toHttpMethod())) {
            return new ResponseParser(getContentAsString());
        } else throw new PluginImplementationException("No JSon object response");
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }
}
