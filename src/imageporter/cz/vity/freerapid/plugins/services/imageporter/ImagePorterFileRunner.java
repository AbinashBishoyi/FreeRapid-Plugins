package cz.vity.freerapid.plugins.services.imageporter;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class ImagePorterFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ImagePorterFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "Filename:</b></td><td nowrap>", "</td>");
        PlugUtils.checkFileSize(httpFile, content, "Size:</b></td><td>", "<small>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String content = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(content);//extract file name and size from the page

            method = getGetMethod(PlugUtils.getStringBetween(content, "<script type=\"text/javascript\" src=\"", "\"></script><script type=\"text/javascript\">"));
            setFileStreamContentTypes(new String[0], new String[]{"application/x-javascript"});
            if (makeRedirectedRequest(method)) {
                final String cookieName = PlugUtils.getStringBetween(getContentAsString(), "cookiename: [\"", "\", \"");
                final String displayFreq = "0"; // Integer.parseInt(PlugUtils.getStringBetween(getContentAsString(), "displayfrequency: \"", "\","));
                Matcher match = PlugUtils.matcher("\\[0\\]+\"(.+?)\", \"(.+?)\"\\);", getContentAsString());
                if (match.find()) {
                    final String pageTag = match.group(1);
                    final String pageVal = match.group(2);
                    addCookie(new Cookie("www.imageporter.com", cookieName + pageTag, pageVal, "/", 86400, false));
                }
                addCookie(new Cookie("www.imageporter.com", cookieName, displayFreq, "/", 86400, false));
            }
            final HttpMethod httpMethod = getGetMethod(PlugUtils.getStringBetween(content, "javascript:bookilsfx()\" ><img src=\"", "\" id="));
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
        if (contentAsString.contains("The file you were looking for could not be found") ||
                contentAsString.contains("No such file with this filename") ||
                contentAsString.contains("The file was deleted") ||
                contentAsString.contains("The file expired")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}