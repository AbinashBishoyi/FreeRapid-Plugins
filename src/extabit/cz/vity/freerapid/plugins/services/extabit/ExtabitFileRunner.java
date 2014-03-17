package cz.vity.freerapid.plugins.services.extabit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
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
class ExtabitFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ExtabitFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        if (makeFirstPossiblyRedirectedRequest()) {
            checkFileProblems();
            checkNameAndSize();//ok let's extract file name and size from the page
        } else
            throw new ServiceConnectionProblemException();
    }

		/**
		 * @return
		 * @throws IOException
		 * @throws ServiceConnectionProblemException
		 */
		private boolean makeFirstPossiblyRedirectedRequest() throws IOException {
			final GetMethod getMethod = getGetMethod(fileURL);//make first request
			if (!makeRequest(getMethod)) {
				if(getMethod.getStatusCode()/100==3) {
					fileURL = getMethod.getResponseHeader("Location").getValue();
					logger.info(String.format("Redirected, changing URL to %s", fileURL));
					GetMethod m2 = getGetMethod(fileURL);
					return makeRequest(m2);
				}
				return false;
			}
			return true;
		}

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
    	Matcher nameMatcher = PlugUtils.matcher("<[^>]*id=\"download_filename\"[^>]*>([^<>]+)<", getContentAsString());
    	if(!nameMatcher.find())
    		unimplemented();
    	httpFile.setFileName(nameMatcher.group(1));

    	Matcher sizeMatcher = PlugUtils.matcher("Размер файла:(?:\\s|<[^<>]*>)*([^<>]+)<", getContentAsString());
    	if(!sizeMatcher.find())
    		unimplemented();
    	httpFile.setFileSize(PlugUtils.getFileSizeFromString(sizeMatcher.group(1)));
    	httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();
        checkDownloadProblems();
        
        final HttpMethod req1 = getMethodBuilder()
        	.setAction("?go")
        	.setBaseURL(fileURL)
        	.toHttpMethod();
        if(!makeRequest(req1))
        	throw new ServiceConnectionProblemException();
        checkProblems();
        
        final HttpMethod req2 = getMethodBuilder()
        	.setActionFromFormByName("cmn_form", true)
        	.setParameter("capture", getCaptchaCode())
        	.setBaseURL("http://extabit.com/")
        	.toHttpMethod();
        if(!makeRequest(req2))
        	throw new ServiceConnectionProblemException();
        checkProblems();
        
        if(!getContentAsString().contains("\"?af\""))
        	throw new CaptchaEntryInputMismatchException();
        
        final HttpMethod req3 = getMethodBuilder()
        	.setAction("?af")
        	.setBaseURL(fileURL)
        	.toHttpMethod();
        if(!makeRequest(req3))
        	throw new ServiceConnectionProblemException();
        checkProblems();
        
        final HttpMethod req4 = getMethodBuilder()
        	.setActionFromAHrefWhereATagContains("Скачать")
        	.toHttpMethod();
        
        if (!tryDownloadAndSaveFile(req4)) {
        	checkProblems();//if downloading failed
    			logger.warning(getContentAsString());//log the info
    			
    			/* Since the server doesn't properly indicate
    			 * multiple downloads condition, just assume it here
    			 */
    			throw new YouHaveToWaitException("Unknown problem - will try again", 1800);
        }
    }

		/**
		 * @return
		 * @throws PluginImplementationException 
		 * @throws ErrorDuringDownloadingException 
		 */
		private String getCaptchaCode() throws ErrorDuringDownloadingException {
			CaptchaSupport cs=getCaptchaSupport();
			Matcher captchaMatcher=PlugUtils.matcher("<img src=\"(/cap[^\"]*)\"", getContentAsString());
			if(!captchaMatcher.find())
				unimplemented();
			
			return cs.getCaptcha("http://extabit.com"+captchaMatcher.group(1));
		}

		/**
		 * @throws PluginImplementationException
		 */
		private void unimplemented() throws PluginImplementationException {
			logger.warning(getContentAsString());//log the info
			throw new PluginImplementationException();//some unknown problem
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<h1>Файл не найден</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }
    
    private void checkDownloadProblems() {
    }
    
    private void checkProblems() throws ErrorDuringDownloadingException {
    	checkFileProblems();
    	checkDownloadProblems();
    }

}