package cz.vity.freerapid.plugins.services.vip_file;

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
 * @author Thumb
 */
class VipFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(VipFileFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (!makeRedirectedRequest(getMethod))
        	throw new ServiceConnectionProblemException();
        
        checkProblems();
        checkNameAndSize();//ok let's extract file name and size from the page
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
    	Matcher name_match=PlugUtils.matcher("File Name:(?:\\s|<[^>]*>)*([^>]+)<", getContentAsString());
    	if(!name_match.find())
    		unimplemented();
    	
    	Matcher URL_match=PlugUtils.matcher(".*/([^/]+)\\.html?", fileURL);
    	if(URL_match.matches())
    		httpFile.setFileName(URL_match.group(1));
    	
    	Matcher size_match=PlugUtils.matcher("Size:(?:\\s|<[^>]*>)*([^>]+)<", getContentAsString());
    	if(!size_match.find())
    		unimplemented();
    	httpFile.setFileSize(PlugUtils.getFileSizeFromString(size_match.group(1)));

    	httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();
       
        final HttpMethod httpMethod = getMethodBuilder()
        	.setActionFromAHrefWhereATagContains("download with Very Slow Speed")
        	.toHttpMethod();

        //here is the download link extraction
        if (!tryDownloadAndSaveFile(httpMethod)) {
        	checkProblems();//if downloading failed
        	unimplemented();
        }
    }

    /**
     * @throws PluginImplementationException 
		 * 
		 */
		private void unimplemented() throws PluginImplementationException {
			logger.warning(getContentAsString());
			throw new PluginImplementationException();
		}

		private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains(">This file not found<")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}