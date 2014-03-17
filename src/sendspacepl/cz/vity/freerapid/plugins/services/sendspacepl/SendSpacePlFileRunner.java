package cz.vity.freerapid.plugins.services.sendspacepl;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Eterad
 */
class SendSpacePlFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SendSpacePlFileRunner.class.getName());


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
        checkProblems();
        PlugUtils.checkName(httpFile, content, "<span class=\"blue4\" style=\"font-size: 12px;\"><b>", "</b></span></div>");
        PlugUtils.checkFileSize(httpFile, content, "<div class=\"info\"><span class=\"blue4\">", "</span></div>");//
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            final Matcher matcher = getMatcherAgainstContent("href=\"([^\"]+)\"><img src=\"http://www.sendspace.pl/media/img/pobierz_grey.gif\" alt=\"Pobierz plik\" title=\"Pobierz plik\" />");
            if(!matcher.find()){
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Content not found");
            }
            final String URL = matcher.group(1);
            logger.info(">>>>> "+URL+" <<<<<");
            client.setReferer(fileURL);
            method = getGetMethod(URL);
            final int resultCode = client.makeRequest(method, false);
            if (!isRedirect(resultCode))
                throw new PluginImplementationException("Redirect not found");
            final Header responseLocation = method.getResponseHeader("Location");//Location does not return correct URL
            if (responseLocation == null)
                throw new PluginImplementationException("Location header not found");
            logger.info(">>>>> Location header: "+responseLocation.getValue()+" <<<<<");
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(responseLocation.getValue()).toGetMethod();
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
        if (contentAsString.contains("Podany plik nie istnieje")) {//
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    protected boolean isRedirect(int statuscode) {
        return (statuscode == HttpStatus.SC_MOVED_TEMPORARILY) ||
                (statuscode == HttpStatus.SC_MOVED_PERMANENTLY) ||
                (statuscode == HttpStatus.SC_SEE_OTHER) ||
                (statuscode == HttpStatus.SC_TEMPORARY_REDIRECT);
    }

}
