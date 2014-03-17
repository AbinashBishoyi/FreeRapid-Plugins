package cz.vity.freerapid.plugins.services.addat;

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
 * @author Javi
 */
class AddatFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AddatFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final String downloadURL = addaturl(fileURL);
        final GetMethod getMethod = getGetMethod(downloadURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<span style=\"font-size: 13px; font-weight: bolder;\">", "</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        final String downloadURL = addaturl(fileURL);
        logger.info("Starting download in TASK " + downloadURL);
        final GetMethod method = getGetMethod(downloadURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            final String churl = check(downloadURL);
            final GetMethod getMethod = getGetMethod(churl);//make first request
            if (makeRedirectedRequest(getMethod)) {
                checkProblems();
            } else
                throw new PluginImplementationException();

            final HttpMethod httpMethod = getGetMethod(downloadURL.replaceFirst("/freedownload", ""));

            client.getHTTPClient().getParams().setParameter("considerAsStream", "text/plain; charset=us-ascii");
            //here is the download link extraction
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
        if (contentAsString.contains("nem található")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private String addaturl(String url) throws ErrorDuringDownloadingException {
        Matcher matcher = PlugUtils.matcher("addat.hu/(.*?)(/|$)", url);
        if (matcher.find()) {
            url = "http://www.addat.hu/" + matcher.group(1).trim() + "/freedownload";
            logger.info("New url: " + url);
        } else {
            logger.warning("File url not found");
            throw new URLNotAvailableAnymoreException("File not found");
        }
        return url;

    }

    private String check(String url) throws ErrorDuringDownloadingException {
        Matcher matcher = PlugUtils.matcher("addat.hu/(.*?)/", url);
        if (matcher.find()) {
            url = "http://addat.hu/stmch.php?id=" + matcher.group(1).trim();
            logger.info("Check url: " + url);
        } else {
            logger.warning("File url not found");
            throw new URLNotAvailableAnymoreException("File not found");
        }
        return url;
    }


}