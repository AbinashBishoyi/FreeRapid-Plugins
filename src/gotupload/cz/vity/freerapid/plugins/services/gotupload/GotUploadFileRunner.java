package cz.vity.freerapid.plugins.services.gotupload;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Javi
 */
class GotUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GotUploadFileRunner.class.getName());


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
        PlugUtils.checkName(httpFile, content, "Filename:</b></td><td>", "</td>");
        PlugUtils.checkFileSize(httpFile, content, "Size:</b></td><td>", "<small>");
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
            Matcher redir = PlugUtils.matcher("name=\"id\" value=\"([^\"]+)\"", getContentAsString());
            if (!redir.find()) {
                throw new PluginImplementationException();
            }
            String redirname = redir.group(1);
            PostMethod postmethod = getPostMethod(fileURL);
            postmethod.addParameter("method_free", "Free Download");
            postmethod.addParameter("op", "download1");
            postmethod.addParameter("id", redirname);
            if (makeRedirectedRequest(postmethod)) {
                checkProblems();
                Matcher rand = PlugUtils.matcher("name=\"rand\" value=\"([^\"]+)\"", getContentAsString());
                if (!rand.find()) {
                    throw new PluginImplementationException();
                }
                String random = rand.group(1);
                PostMethod getmethod = getPostMethod(fileURL);
                getmethod.addParameter("method_free", "Free Download");
                getmethod.addParameter("op", "download2");
                getmethod.addParameter("id", redirname);
                getmethod.addParameter("rand", random);
                getmethod.addParameter("referer", fileURL);
                getmethod.addParameter("btn_download", "Sending File...");
                if (!tryDownloadAndSaveFile(getmethod)) {
                    checkProblems();//if downloading failed
                    logger.warning(getContentAsString());//log the info
                    throw new PluginImplementationException();//some unknown problem
                }
            } else {
                throw new PluginImplementationException("Bad login URL");
            }

            //here is the download link extraction

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException, YouHaveToWaitException {
        final String contentAsString = getContentAsString();
        Matcher matcher;//Your IP address XXXXXX is already downloading a file.  Please wait until the download is completed.
        if (getContentAsString().contains("You have to wait")) {
            matcher = getMatcherAgainstContent("You have to wait ([0-9]+) minute");
            if (matcher.find()) {
                throw new YouHaveToWaitException("You have reached the download limit", Integer.parseInt(matcher.group(1)) * 60 + 59);
            }
            throw new ServiceConnectionProblemException("You have reached the download-limit for free-users.");
        }
        if (contentAsString.contains("You have reached the download")) {
            throw new ErrorDuringDownloadingException("You have reached the download-limit for free-users.");
        }
        if (contentAsString.contains("Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}