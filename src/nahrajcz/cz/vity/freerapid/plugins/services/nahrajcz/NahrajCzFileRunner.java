package cz.vity.freerapid.plugins.services.nahrajcz;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class NahrajCzFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NahrajCzFileRunner.class.getName());


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
        Matcher matcher = PlugUtils.matcher("<span class=\"title\">\\s*(.+?)\\s*</span>", content);
        //Matcher fileNameFromURLMatcher = PlugUtils.matcher("<div class=\"download\"><a href=\".+/file/\\d+\\-(.+?)\"", content);
        if (matcher.find()) {
            final String fileName = matcher.group(1).trim(); //method trim removes white characters from both sides of string
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);
            matcher = PlugUtils.matcher("<span class=\"size\">\\s*(.+?)</span>", content);
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
            client.setReferer(fileURL);//prevention - some services checks referers
            //here is the download link extraction
            final Matcher matcher = getMatcherAgainstContent("style=\"display:none\" method=\"post\" enctype=\"multipart/form-data\" action=\"(.+?)\"");
            if (matcher.find()) {
                System.out.println("fileURL" + matcher.group(1));
                final GetMethod postMethod = getGetMethod(matcher.group(1));//GET request will do fine
                //PlugUtils.addParameters(postMethod, getContentAsString(), new String[]{"sid"});
                if (!tryDownloadAndSaveFile(postMethod)) {
                    checkProblems();//if downloading failed
                    logger.warning(getContentAsString());//log the info
                    throw new PluginImplementationException();//some unknown problem
                }
            } else throw new PluginImplementationException("Plugin error: Download link not found");
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();    
        if (contentAsString.contains("Nahraj.cz - Neznam")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}
