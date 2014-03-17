package cz.vity.freerapid.plugins.services.bigshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Thumb
 */
class BigShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BigShareFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
    	super.runCheck();
    	final GetMethod getMethod = getGetMethod(fileURL);//make first request
    	myMakeRequest(getMethod);
    	checkNameAndSize();//ok let's extract file name and size from the page
    }

    private final void checkNameAndSize() throws ErrorDuringDownloadingException {
    	Matcher fn_match=PlugUtils.matcher("File name:(?:</b>)?</td>\\s*<td[^<>]*>((?:(?!</td>).)*)", getContentAsString());
      // they have a little XSS vulnerability here
    	if(fn_match.find()) {
    		httpFile.setFileName(fn_match.group(1));
    	} else
    		unimplemented("Failed to get filename");
    	
    	Matcher fs_match=PlugUtils.matcher("File size:(?:</b>)?</td>\\s*<td[^<>]*>([^<>]*)<", getContentAsString());
    	if(fs_match.find()) {
    		httpFile.setFileSize(PlugUtils.getFileSizeFromString(fs_match.group(1)));
    	} else
    		unimplemented("Failed to get size");
    	httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();//extract file name and size from the page
        
        Matcher link_matcher=PlugUtils.matcher("<textarea[^<>]*>([^<>]*)", getContentAsString());
        if(!link_matcher.find())
        	unimplemented("Couldn't find the download URL");
        
        final HttpMethod httpMethod = getMethodBuilder()
        	.setAction(link_matcher.group(1))
        	.toHttpMethod();

        //here is the download link extraction
        if (!tryDownloadAndSaveFile(httpMethod)) {
        	checkProblems();
        	unimplemented("Final download fail");
        }
    }

    private final void myMakeRequest(GetMethod m) throws IOException, ErrorDuringDownloadingException {
    	m.setFollowRedirects(true);
    	makeRequest(m);
    	if(m.getStatusCode() >= 400 || m.getQueryString().contains("error=1")) {
    		if(m.getQueryString().contains("DL_FileNotFound"))
    			throw new URLNotAvailableAnymoreException("File not found");
    		if(m.getStatusCode() >= 400)
    			throw new ServiceConnectionProblemException(String.format("Error making request, status %s", m.getStatusLine()));
    		unimplemented(String.format("myMakeRequest: error connecting, statusCode %s", m.getStatusLine()));
    	}
    	checkProblems();
    }
    
    private final void unimplemented(String detail) throws PluginImplementationException {
    	logger.warning(getContentAsString());//log the info
    	throw new PluginImplementationException(detail);    	
    }

    protected final void checkProblems() throws ErrorDuringDownloadingException {
    	if(getContentAsString().contains("<span>You have got max allowed download sessions from the same IP!</span>"))
    		throw new YouHaveToWaitException("Download limit exceeded", 1800);
    }
}
