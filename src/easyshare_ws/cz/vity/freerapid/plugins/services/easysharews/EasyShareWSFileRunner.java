package cz.vity.freerapid.plugins.services.easysharews;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

/**
 * Class which contains main code
 *
 * @author Thumb
 */
class EasyShareWSFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(EasyShareWSFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new ServiceConnectionProblemException();
    }

    private final void checkNameAndSize(final String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h2>Download File ", "</h2>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();
        
        final HttpMethod httpMethod = getMethodBuilder()
        	.setActionFromFormByIndex(1, true)
        	.setAction(fileURL)
        	.setParameter("method_premium", null)
        	.toPostMethod();
        
        if(!makeRedirectedRequest(httpMethod))
        	throw new ServiceConnectionProblemException();
        
        checkProblems();
        waitForTime();
        
        final HttpMethod httpMethod2 = getMethodBuilder()
        	.setActionFromFormByName("F1", true)
        	.setAction(fileURL)
//        	.setParameter("captcha_code", getCaptchaCode())
        	.toPostMethod();
        
        if (!tryDownloadAndSaveFile(httpMethod2)) { //we make the main request
        	checkProblems();//if downloading failed
        	unimplemented();
        }
    }

    private final void checkProblems() throws ErrorDuringDownloadingException {
    	if(PlugUtils.matcher("<[^<>]* class=\"err\"[^<>]*>", getContentAsString()).find()) {
    		final Matcher m=PlugUtils.matcher("You have to wait (\\d+) minutes, (\\d+) seconds", getContentAsString());
    		if(m.find()) {
    			throw new YouHaveToWaitException("Wait", Integer.valueOf(m.group(1))*60 + Integer.valueOf(m.group(2)));
    		}
    		if(getContentAsString().contains("No such file with this filename"))
    			throw new URLNotAvailableAnymoreException();
    		if(getContentAsString().contains("Skipped countdown"))
    			throw new YouHaveToWaitException("Skipped countdown", 10);
    		unimplemented();
    	}
    	if(getContentAsString().contains("<b>File Not Found</b>"))
    		throw new URLNotAvailableAnymoreException();
    }

    private final String getCaptchaCode() throws PluginImplementationException {
    	final Matcher m=PlugUtils.matcher("(?s:Enter code below:(.*)class=\"captcha_code\")", getContentAsString());
    	if(!m.find())
    		unimplemented();
    	
    	// TODO this is wrong. Nevertheless, it works
    	// basically, they allow anything as captcha (for now)
			final Pattern p=Pattern.compile("<[^>]*(>|$)");
			final StringBuilder builder=new StringBuilder();
			for(String part : p.split(m.group(1)))
				builder.append(part);
			
			return builder.toString().trim();
    	
    }
    
    private final void unimplemented() throws PluginImplementationException {
    	logger.warning(getContentAsString());//log the info
    	throw new PluginImplementationException();//some unknown problem
    }
    
    private final void waitForTime() throws ErrorDuringDownloadingException, InterruptedException {
    	Matcher timeMatcher=PlugUtils.matcher("<span id=\"countdown\">(\\d+)</span>", getContentAsString());
    	if(!timeMatcher.find())
    		unimplemented();
    	try {
    		downloadTask.sleep(Integer.valueOf(timeMatcher.group(1)));
    	} catch(NumberFormatException e) {
    		unimplemented();
    	}
    }
}
