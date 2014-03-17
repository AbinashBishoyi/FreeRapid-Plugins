package cz.vity.freerapid.plugins.services.gigapeta;

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
class GigaPetaFileRunner extends AbstractRunner {
	private final static Logger logger = Logger.getLogger(GigaPetaFileRunner.class.getName());


	@Override
	public void runCheck() throws Exception { //this method validates file
		super.runCheck();
		final GetMethod getMethod = getGetMethod(fileURL);//make first request
		if (!makeRedirectedRequest(getMethod))
			throw new ServiceConnectionProblemException();

		checkFileProblems();
		checkNameAndSize();//ok let's extract file name and size from the page
	}

	private void checkNameAndSize() throws ErrorDuringDownloadingException {
		//final Matcher name_match=PlugUtils.matcher("<tr class=\"name\">(?:\\s|<[^>]*>)*(.+?)\\s*</t[rd]>", getContentAsString());
		final Matcher namefile_match=PlugUtils.matcher("<table id=\"download\">(?:\\s|<[^>]*>)*(.+?)\\s*</t[rd]>(?:\\s|<[^>]*>)*(?:[^<>]+)\\s*(?:\\s|<[^>]*>)*([^<>]+)\\s*<", getContentAsString());
		if(!namefile_match.find())
			unimplemented();

		httpFile.setFileName(namefile_match.group(1));

		/* This code is not necessary - new regex cat file size
		final Matcher size_match=PlugUtils.matcher("Размер(?:\\s|<[^>]*>)*([^<>]+)\\s*<", getContentAsString());
		if(!size_match.find())
			unimplemented();
		/**/
		httpFile.setFileSize(PlugUtils.getFileSizeFromString(namefile_match.group(2)));
		httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
	}

	/**
	 * @throws PluginImplementationException
	 *
	 */
	private void unimplemented() throws PluginImplementationException {
		logger.warning(getContentAsString());
		throw new PluginImplementationException();
	}

	@Override
	public void run() throws Exception {
		super.run();
		logger.info("Starting download in TASK " + fileURL);
		runCheck();
		checkDownloadProblems();

		downloadTask.sleep(10);

		String captcha_id=String.format("%d", (int)Math.ceil(Math.random()*1e8));

		final HttpMethod httpMethod = getMethodBuilder()
			.setParameter("captcha_key", captcha_id)
			.setParameter("captcha", getCaptcha(captcha_id))
			.setParameter("download", "\u00D0\u00A1\u00D0\u00BA\u00D0\u00B0\u00D1\u2021\u00D0\u00B0\u00D1\u201A\u00D1\u0152")
			.setAction(fileURL)
			.toPostMethod();

		//here is the download link extraction
		if (!tryDownloadAndSaveFile(httpMethod)) {
			checkProblems();//if downloading failed
			unimplemented();
		}
	}

	private void checkDownloadProblems() throws ErrorDuringDownloadingException {
		final String contentAsString = getContentAsString();
		if (contentAsString.contains("<div id=\"page_error\">")) {
			if(contentAsString.contains("\u00D0\u00A6\u00D0\u00B8\u00D1\u201E\u00D1\u20AC\u00D1\u2039 \u00D1\uFFFD \u00D0\u00BA\u00D0\u00B0\u00D1\u20AC\u00D1\u201A\u00D0\u00B8\u00D0\u00BD\u00D0\u00BA\u00D0\u00B8 \u00D0\u00B2\u00D0\u00B2\u00D0\u00B5\u00D0\u00B4\u00D0\u00B5\u00D0\u00BD\u00D1\u2039 \u00D0\u00BD\u00D0\u00B5\u00D0\u00B2\u00D0\u00B5\u00D1\u20AC\u00D0\u00BD\u00D0\u00BE"))
				throw new CaptchaEntryInputMismatchException();
			if(PlugUtils.matcher("\u00D0\u2019\u00D1\uFFFD\u00D0\u00B5 \u00D0\u00BF\u00D0\u00BE\u00D1\u201A\u00D0\u00BE\u00D0\u00BA\u00D0\u00B8 \u00D0\u00B4\u00D0\u00BB\u00D1\uFFFD IP [0-9.]* \u00D0\u00B7\u00D0\u00B0\u00D0\u00BD\u00D1\uFFFD\u00D1\u201A\u00D1\u2039", contentAsString).find())
				throw new YouHaveToWaitException("Download streams for your IP exhausted", 1800);
			if(contentAsString.contains("\u00D0\u2019\u00D0\u00BD\u00D0\u00B8\u00D0\u00BC\u00D0\u00B0\u00D0\u00BD\u00D0\u00B8\u00D0\u00B5! \u00D0\u201D\u00D0\u00B0\u00D0\u00BD\u00D0\u00BD\u00D1\u2039\u00D0\u00B9 \u00D1\u201E\u00D0\u00B0\u00D0\u00B9\u00D0\u00BB \u00D0\u00B1\u00D1\u2039\u00D0\u00BB \u00D1\u0192\u00D0\u00B4\u00D0\u00B0\u00D0\u00BB\u00D0\u00B5\u00D0\u00BD"))
				throw new URLNotAvailableAnymoreException("File was deleted");
			unimplemented();
		}
	}

	private void checkFileProblems() throws ErrorDuringDownloadingException {
		Matcher err_match=PlugUtils.matcher("<h1 class=\"big_error\">([^>]+)</h1>", getContentAsString());
		if(err_match.find()) {
			if(err_match.group(1).equals("404"))
				throw new URLNotAvailableAnymoreException("File not found");
			unimplemented();
		}
	}

	private void checkProblems() throws ErrorDuringDownloadingException {
		checkFileProblems();
		checkDownloadProblems();
	}

	private String getCaptcha(String id) throws ErrorDuringDownloadingException {
		final CaptchaSupport captchas=getCaptchaSupport();
		final String ret=captchas.getCaptcha("http://gigapeta.com/img/captcha.gif?x="+id);
		if (ret==null)
			throw new CaptchaEntryInputMismatchException();
		return ret;
	}

}