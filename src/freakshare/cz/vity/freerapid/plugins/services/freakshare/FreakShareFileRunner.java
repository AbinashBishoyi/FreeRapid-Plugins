package cz.vity.freerapid.plugins.services.freakshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Thumb
 */
class FreakShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FreakShareFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
    	super.runCheck();
    	final GetMethod getMethod = getGetMethod(fileURL);//make first request
    	if (makeRedirectedRequest(getMethod)) {
    		checkProblems();
    		Matcher m=PlugUtils.matcher("/([^/]*)$", fileURL);
    		if(!m.find())
    			unimplemented(String.format("Error getting filename from URL: %s", fileURL));
    		httpFile.setFileName(m.group(1));
    		httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    	} else
    		throw new ServiceConnectionProblemException();
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();

        waitForTime();
        final HttpMethod httpMethod = getMethodBuilder()
        	.setActionFromFormWhereActionContains("http://freakshare.net/files", true)
        	.toHttpMethod();
            

        if(!makeRequest(httpMethod))
        	throw new ServiceConnectionProblemException();
        checkProblems();
        waitForTime();

        final HttpMethod httpMethod2 = getMethodBuilder()
        	.setActionFromFormWhereActionContains("http://freakshare.net/files", true)
        	.toHttpMethod();
        
        /* The last request is redirected. However, we cannot use
         * normal redirection infrastructure, as the server sends
         * binary data as text/plain, and on top of that, with gzip
         * content-encoding. This would cause OOM while decoding
         * the gzip, therefore, we need to reject that encoding
         * with the http header, which cannot be done for
         * automatically redirected request.
         */
        makeRequest(httpMethod2);

        if(httpMethod2.getStatusCode() / 100 != 3)
        	unimplemented(String.format("Unexpected status line: %s", httpMethod2.getStatusLine()));
        
        final HttpMethod httpMethod3=getGetMethod(httpMethod2.getResponseHeader("Location").getValue());
        	
       	httpMethod3.setRequestHeader("Accept-Encoding", "");
        client.getHTTPClient().getParams().setParameter("considerAsStream", "text/plain");

        //here is the download link extraction
        if (!tryDownloadAndSaveFile(httpMethod3)) {
        	checkProblems();
        	unimplemented("Failed to get the file");
        }
    }

    private final void waitForTime() throws InterruptedException {
    	Matcher m=PlugUtils.matcher("var +time *= *([0-9]+(\\.[0-9]*)?)", getContentAsString());
    	if(!m.find()) return;
    	downloadTask.sleep((int)(double)Double.valueOf(m.group(1)));
    }
    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        Matcher matcher=PlugUtils.matcher("<h1[^<>]*>[^<>]*Error[^<>]*</h1>[^<>]*<div[^<>]*>([^<>]*)", contentAsString);
        if(matcher.find()) {
        	String detail=matcher.group(1);
        	if(PlugUtils.matcher("doesn.?t\\s+exist", detail).find())
        		throw new URLNotAvailableAnymoreException(String.format("Error: %s", detail)); //let to know user in FRD
        	if(PlugUtils.matcher("can.?t\\s+download\\s+more\\s+th.n", detail).find())
        		throw new YouHaveToWaitException(String.format("You have to wait: %s", detail), 1800); // wait 30 minutes (as we have no way to tell how long)
        	unimplemented(String.format("Unknown error: %s", detail));
        }
    }
    
    private final void unimplemented(String detail) throws PluginImplementationException {
    	logger.warning(getContentAsString());//log the info
    	throw new PluginImplementationException(detail);//some unknown problem
    }

}
