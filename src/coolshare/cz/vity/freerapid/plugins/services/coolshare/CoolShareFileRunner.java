package cz.vity.freerapid.plugins.services.coolshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
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
class CoolShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CoolShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h1>Stáhnout ", "</h1>");
        PlugUtils.checkFileSize(httpFile, content, "Velikost: <strong>", "</strong>");
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

            String FileID = PlugUtils.getStringBetween(contentAsString, "startFreeDownload(this, &quot;", "&quot;)");

            //try to get base of link from javascript file
            String jsUrl = "http://www.coolshare.cz" + PlugUtils.getStringBetween(contentAsString, "text/javascript\" src=\"", "\">");
            final HttpMethod jsMethod = getMethodBuilder()
                    .setAction(jsUrl)
                    .setEncoding("binary")
                    .toHttpMethod();
            setFileStreamContentTypes(new String[0], new String[]{"application/x-javascript"});
            if (!makeRequest(jsMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error finding download");//some unknown problem
            }
            String dwnlURI = PlugUtils.getStringBetween(getContentAsString(), "link='", "'");

            final HttpMethod httpMethod = getGetMethod(dwnlURI + FileID);
            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
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
        if (contentAsString.contains("Soubor nenalezen")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}