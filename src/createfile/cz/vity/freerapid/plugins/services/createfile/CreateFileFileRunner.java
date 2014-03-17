package cz.vity.freerapid.plugins.services.createfile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Thumb
 */
class CreateFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CreateFileFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize();//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher nameMatcher = PlugUtils.matcher("File name[^<]*(?:\\s|<[^<>]*>)*([^<]*)<", getContentAsString());
        if(!nameMatcher.find())
            unimplemented();
        httpFile.setFileName(nameMatcher.group(1));

        Matcher sizeMatcher = PlugUtils.matcher("File size[^<]*(?:\\s|<[^<>]*>)*([^<]*)<", getContentAsString());
        if(!sizeMatcher.find())
            unimplemented();
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(sizeMatcher.group(1)+"B"));

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        runCheck();
        checkDownloadProblems();

        Matcher hashMatcher = PlugUtils.matcher("/([0-9a-f]*)\\.", fileURL);
        if(!hashMatcher.find())
            unimplemented();
        String fileHash = hashMatcher.group(1);

        final HttpMethod ajax1Method = getMethodBuilder()
            .setAction("http://creafile.com/handlers.php?h=godownl")
//            .setParameter("h", "godownl")
            .setParameter("hash", fileHash)
            .setParameter("captcha", getCaptcha())
            .toPostMethod();
        if (!makeRequest(ajax1Method))
            throw new ServiceConnectionProblemException();
        
        GetMethod refreshMethod = getGetMethod(fileURL);
        if (!makeRequest(refreshMethod))
            throw new ServiceConnectionProblemException();
        if(getContentAsString().contains("For file downloading it is required"))
        	throw new CaptchaEntryInputMismatchException();
        
//        downloadTask.sleep(62);
        final HttpMethod ajax2Method = getMethodBuilder()
            .setAction("http://creafile.com/handlers.php?h=getdownloadarea")
//            .setParameter("h", "getdownloadarea")
            .toPostMethod();
        if (!makeRequest(ajax2Method))
            throw new ServiceConnectionProblemException();

        final HttpMethod fileRequest = getMethodBuilder()
            .setActionFromAHrefWhereATagContains("Download (")
            .toGetMethod();

        //here is the download link extraction
        if (!tryDownloadAndSaveFile(fileRequest)) {
        	if(getContentAsString().isEmpty())
        		throw new YouHaveToWaitException("Unknown problem. Will try later.", 1800);
            checkProblems();//if downloading failed
            unimplemented();
        }
    }

    private void unimplemented() throws PluginImplementationException {
        logger.warning(getContentAsString());//log the info
        throw new PluginImplementationException();//some unknown problem
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        checkDownloadProblems();
    }

    private String getCaptcha() throws FailedToLoadCaptchaPictureException {
    	CaptchaSupport cs=getCaptchaSupport();
    	return cs.getCaptcha("http://creafile.com/codeimg.php");
    }

}