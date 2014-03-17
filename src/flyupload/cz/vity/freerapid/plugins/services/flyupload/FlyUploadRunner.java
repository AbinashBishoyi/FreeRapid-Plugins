package cz.vity.freerapid.plugins.services.flyupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Vity
 * @since 0.82
 */
class FlyUploadRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FlyUploadRunner.class.getName());
    private final static String baseURL = "http://www.flyupload.com";


    private void setEncoding() {
        client.getHTTPClient().getParams().setParameter("pageCharset", "Windows-1250");
        client.getHTTPClient().getParams().setHttpElementCharset("Windows-1250");
    }

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        setEncoding();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        try {
            PlugUtils.checkName(httpFile, content, "Filename:</td><td style=\"border-top: 1px none #cccccc;\">", "<");
            PlugUtils.checkFileSize(httpFile, content, "File Size:</td><td style=\"border-top: 1px dashed #cccccc;\">", "<");
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } catch (PluginImplementationException e) {
            checkProblems();
            throw e;
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        setEncoding();
        logger.info("Starting download in TASK " + fileURL);
        if (!fileURL.toLowerCase().contains("&c=1"))
            fileURL = fileURL + "&c=1";
        client.setReferer(fileURL);
        addCookie(new Cookie(".flyupload.com", "downloadpages", "1", "/", 86400, false));
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            //here is the download link extraction
            final String link = PlugUtils.getStringBetween(getContentAsString(), "+2><A HREF=\"", "\">Download Now</A></FONT>");
            final HttpMethod httpMethod = getMethodBuilder().setAction(link).setReferer(fileURL).toHttpMethod();
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
        if (contentAsString.contains("has either expired or the URL has an invalid fid.")) {
            throw new URLNotAvailableAnymoreException("The file you requested has either expired or the URL has an invalid fid."); //let to know user in FRD
        }
    }

    @Override
    protected String getBaseURL() {
        return baseURL;
    }
}
