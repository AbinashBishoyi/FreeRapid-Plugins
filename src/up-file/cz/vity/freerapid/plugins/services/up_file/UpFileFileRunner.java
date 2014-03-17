package cz.vity.freerapid.plugins.services.up_file;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Thumb
 */
class UpFileFileRunner extends AbstractRunner {
	private final static Logger logger = Logger.getLogger(UpFileFileRunner.class.getName());


	@Override
	public void runCheck() throws Exception { //this method validates file
		super.runCheck();
		final GetMethod getMethod = getGetMethod(fileURL);//make first request
		if (makeRedirectedRequest(getMethod)) {
			checkFileProblems();
			checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
		} else
			throw new ServiceConnectionProblemException();
	}

	private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
		PlugUtils.checkName(httpFile, content, "input type=\"hidden\" name=\"realname\" value=\"", "\"");
		PlugUtils.checkFileSize(httpFile, content, "<span>File`s size::</span>", "<");
		httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
	}

	@Override
	public void run() throws Exception {
		super.run();
		logger.info("Starting download in TASK " + fileURL);
		runCheck();
		checkDownloadProblems();

		final HttpMethod httpMethod = getMethodBuilder()
			.setActionFromFormByName("Premium", true)
			.setBaseURL("http://www.up-file.com")
			.toHttpMethod();

		//here is the download link extraction
		if (!tryDownloadAndSaveFile(httpMethod)) {
			checkProblems();//if downloading failed
			unimplemented();
		}
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
		if (contentAsString.contains("The requested file is not found")) {
			throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
		}
		if (contentAsString.contains("Sorry, this file deleted")) {
			throw new URLNotAvailableAnymoreException("File deleted");
		}

	}

	private void checkDownloadProblems() throws ErrorDuringDownloadingException {
		if (getContentAsString().contains("Downloading is in process from your IP-Address."))
			throw new YouHaveToWaitException("You are already downloading a file", 1800);
	}

	private void checkProblems() throws ErrorDuringDownloadingException {
		checkFileProblems();
		checkDownloadProblems();
	}
}