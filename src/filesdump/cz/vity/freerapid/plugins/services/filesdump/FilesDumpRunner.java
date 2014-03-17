package cz.vity.freerapid.plugins.services.filesdump;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Vity
 * @since 0.82
 */
class FilesDumpRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilesDumpRunner.class.getName());
    private final static String baseURL = "http://www.filesdump.com";

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
        Matcher matcher = PlugUtils.matcher("class=\"file_name\">(.+?)</", content);
        if (matcher.find()) {
            final String fileName = matcher.group(1).trim(); //method trim removes white characters from both sides of string
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);
            //: <strong>204800</strong>KB<br>
            matcher = PlugUtils.matcher("file_size\" style=\"white-space:nowrap\">\\((.+?)\\)</div>", content);
            if (matcher.find()) {
                final long size = PlugUtils.getFileSizeFromString(matcher.group(1));
                httpFile.setFileSize(size);
            } else {
                checkProblems();
                logger.warning("File size was not found\n:");
                throw new PluginImplementationException();
            }
        } else {
            checkProblems();
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }
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
            //here is the download link extraction
            final MethodBuilder methodBuilder = getMethodBuilder();
            final HttpMethod httpMethod = methodBuilder.setActionFromTextBetween("$ld('", "',").setReferer(fileURL).toHttpMethod();
            final String s = httpMethod.getURI().toString();
            logger.info("URI: " + s);
            if (makeRedirectedRequest(httpMethod)) {
                logger.info("CONTENT:" + getContentAsString());
                final Matcher matcher = getMatcherAgainstContent("download the file:<br><br><a href=\"(http.+?)\">");
                if (matcher.find()) {
                    final GetMethod getMethod = getGetMethod(matcher.group(1));//we make POST request for file
                    if (!tryDownloadAndSaveFile(getMethod)) {
                        checkProblems();//if downloading failed
                        logger.warning(getContentAsString());//log the info
                        throw new PluginImplementationException();//some unknown problem
                    }
                } else throw new PluginImplementationException("Plugin error: Download link not found");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Error : 404")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("400 Bad Request")) {
            throw new InvalidURLOrServiceProblemException("400 Bad Request"); //let to know user in FRD
        }
    }

    @Override
    protected String getBaseURL() {
        return baseURL;
    }
}
