package cz.vity.freerapid.plugins.services.u115;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Meow, tonyk, Heend
 */
class u115FileRunner extends AbstractRunner {
	private final static Logger logger = Logger.getLogger(u115FileRunner.class.getName());

	@Override
	public void runCheck() throws Exception {
		super.runCheck();
		final GetMethod getMethod = getGetMethod(fileURL);

		if (makeRedirectedRequest(getMethod)) {
			checkProblems();
			checkNameAndSize(getContentAsString());
		} else {
			checkProblems();
			throw new ServiceConnectionProblemException();
		}

	}

	private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
		PlugUtils.checkName(httpFile, content, "<span class=\"file-name\">", "</span>");//TODO
		PlugUtils.checkFileSize(httpFile, content, "<li>\u6587\u4EF6\u5927\u5C0F\uFF1A", "</li>");//TODO
		httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
	}

	@Override
	public void run() throws Exception {
		super.run();
		boolean b = checkISP();
		logger.info("Starting download in TASK " + fileURL);
		final GetMethod method = getGetMethod(fileURL);

		if (makeRedirectedRequest(method)) {
			final String contentAsString = getContentAsString();
			checkProblems();
			checkNameAndSize(contentAsString);

			if (contentAsString.contains("\u6E38\u5BA2\u9700\u7B49\u5F85")) {
				int waitTime = PlugUtils.getWaitTimeBetween(contentAsString, "<b id=\"js_get_download_second\">", "</b>", TimeUnit.SECONDS);
				downloadTask.sleep(waitTime);
			}

			if (b) {
				final HttpMethod method1 = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("\u7535\u4FE1\u4E0B\u8F7D").toHttpMethod();
				if (!tryDownloadAndSaveFile(method1)) {
					checkProblems();//if downloading failed
					logger.warning(getContentAsString());//log the info
					throw new PluginImplementationException();//some unknown problem
				}		
			} else {
				final HttpMethod method1 = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("\u8054\u901A\u4E0B\u8F7D").toHttpMethod();
				if (!tryDownloadAndSaveFile(method1)) {
					checkProblems();//if downloading failed
					logger.warning(getContentAsString());//log the info
					throw new PluginImplementationException();//some unknown problem
				}
			}


		} else {
			checkProblems();
			throw new ServiceConnectionProblemException();
		} 

	}

	//check chinatelecom or chinaunicom
	private boolean checkISP() throws IOException, PluginImplementationException {
		boolean result = false;
		String var = "";
		String checkURL = "http://www.123cha.com/";
		GetMethod method = getGetMethod(checkURL);

		if (makeRequest(method)) {
			try {
				var = PlugUtils.getStringBetween(getContentAsString(), "\u6765\u81EA:  ", " ++");
			} catch  (PluginImplementationException pie) {
			} finally {
				
				if (getContentAsString().contains("\u7535\u4FE1")) {
					result = true;
				} else {
					result = false;
				}
				logger.info("downloading from" + var);
			}

		} else {
			result = false;
		}
		return result;

	}

	private void checkProblems() throws ErrorDuringDownloadingException {
		final String contentAsString = getContentAsString();

		if (contentAsString.contains("id=\"error_message\"") || contentAsString.contains("\u8BBF\u95EE\u7684\u9875\u9762\u4E0D\u5B58\u5728")) {
			throw new URLNotAvailableAnymoreException("File not found");
		}

		if (contentAsString.contains("\u6587\u4EF6\u62E5\u6709\u8005\u672A\u5206\u4EAB\u8BE5\u6587\u4EF6")) {
			throw new InvalidURLOrServiceProblemException("\u9700\u8981\u63D0\u53D6\u7801\u6216\u6587\u4EF6\u6240\u6709\u8005\u672A\u8BBE\u7F6E\u6210\u201C\u5206\u4EAB\u201D"); 
		}	

	}

}