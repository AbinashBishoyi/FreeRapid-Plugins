package cz.vity.freerapid.plugins.services.sendspacepl;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        final Matcher matcher = getMatcherAgainstContent("Nazwa pliku:<br /><a href=\".+\" class=\".+\" style=\".+;\">([^<]+)</a>");
        if (matcher.find()) {
            String fileName = matcher.group(1).trim();
            logger.info("File name " + fileName);
//            final String decoded = checkEncodedFileName(fileName);
//            if (!fileName.equals(decoded)) {
//                logger.info("File name decoded" + decoded);
//                fileName = decoded;
//            }
            httpFile.setFileName(fileName);
        } else {
            throw new PluginImplementationException("File name not found");
        }
        PlugUtils.checkFileSize(httpFile, content, "Rozmiar pliku: <b>", "</b><br />");//
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
            final Matcher matcher = getMatcherAgainstContent("href=\"([^\"]+)\"><img src=\"http://www.sendspace.pl/templates/img/button/download_file.gif\"");
            if(!matcher.find()){
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Content not found");
            }
            final String URL = matcher.group(1);
            client.setReferer(fileURL);
            method = getGetMethod(URL);
            final int resultCode = client.makeRequest(method, false);
            if (!isRedirect(resultCode))
                throw new PluginImplementationException("Redirect not found");
            final Header responseLocation = method.getResponseHeader("Location");//Location does not return correct URL
            if (responseLocation == null)
                throw new PluginImplementationException("Location header not found");
            //getNameFromURL(responseLocation.getValue());
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(responseLocation.getValue()).toHttpMethod();
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

    private void getNameFromURL(String URL) throws PluginImplementationException {
        Pattern p = Pattern.compile("http://sv[0-9]+.sendspace.pl/file/[^/]+/[^/]+/(.+)");
        Matcher m = p.matcher(URL);
        if(!m.find()){
            throw new PluginImplementationException("File name not found in URL");
        }
        String n = m.group(1);
        httpFile.setFileName(n);
    }

}
