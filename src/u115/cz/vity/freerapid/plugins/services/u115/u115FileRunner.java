package cz.vity.freerapid.plugins.services.u115;

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
 * @author Meow
 */
class u115FileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(u115FileRunner.class.getName());


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
        //Matcher matcher=PlugUtils.matcher("<h2 class=\"nowrap file-name.+?>\\s*(.+?)\\s*<",content);
        PlugUtils.checkName(httpFile, content, "<title>", " - 115\u7F51\u7EDCU\u76D8");//TODO
        PlugUtils.checkFileSize(httpFile, content, "\">\u6587\u4EF6\u5927\u5C0F\uFF1A", "</td>");
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
            Matcher matcher=PlugUtils.matcher("^<a href=\"([^\"]+?)\" onclick=\"sendMnvdToServer\\(\\);",contentAsString);


            if(matcher.find()){
                logger.warning(matcher.group(1).trim());
                final HttpMethod httpMethod = getGetMethod(matcher.group(1).trim());
                //here is the download link extraction
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    logger.warning(getContentAsString());//log the info
                    throw new PluginImplementationException();//some unknown problem
                }
            }else{
                throw new PluginImplementationException();    
            }


        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}