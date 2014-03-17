package cz.vity.freerapid.plugins.services.onefichier;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class OneFichierFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(OneFichierFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        setEnglishURL();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            final Matcher match = PlugUtils.matcher("http://(\\w+)\\.(1fichier|desfichiers)\\.com/en/?(.*)", fileURL);
            if (match.find()) {
                String name = match.group(1);
                if (match.group(3).length() > 0)
                    name = URLDecoder.decode(match.group(3), "UTF-8");
                httpFile.setFileName(name);
            }
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void setEnglishURL() {
        if (!fileURL.contains("/en")) {
            String[] temp = fileURL.split(".com");
            fileURL = temp[0] + ".com/en";
            if (temp.length > 1) fileURL += temp[1];
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "name :</th><td>", "</td>");
        PlugUtils.checkFileSize(httpFile, content, "Size :</th><td>", "</td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        setEnglishURL();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final HttpMethod hMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormWhereTagContains("Free Download", true).toPostMethod();
            if (makeRedirectedRequest(hMethod)) {
                final String contentAsString = getContentAsString();//check for response
                checkProblems();//check problems
                checkNameAndSize(contentAsString);//extract file name and size from the page
                //      downloadTask.sleep(1 + PlugUtils.getNumberBetween(contentAsString, "var count =", ";"));
                final HttpMethod h2Method = getMethodBuilder().setReferer(fileURL).setActionFromFormWhereTagContains("Show the download link", true).toPostMethod();
                if (!makeRedirectedRequest(h2Method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();//check problems
                HttpMethod httpMethod;
                try {
                    httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormWhereTagContains("Download the file", true).toPostMethod();
                } catch (Exception e) {
                    httpMethod = getGetMethod(PlugUtils.getStringBetween(getContentAsString(), "window.location = '", "'"));
                }
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file could not be found") ||
                contentAsString.contains("The requested file has been deleted")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("you can download only one file at a time")) {
            final int delay = PlugUtils.getNumberBetween(contentAsString, "wait up to", "minute");
            throw new YouHaveToWaitException("You can download only one file at a time and you must wait up to " + delay + " minutes between each downloads", delay * 60);
        }
    }

}