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
			.setParameter("download", "Ð¡ÐºÐ°Ñ‡Ð°Ñ‚ÑŒ")
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
			if(contentAsString.contains("Ð¦Ð¸Ñ„Ñ€Ñ‹ Ñ� ÐºÐ°Ñ€Ñ‚Ð¸Ð½ÐºÐ¸ Ð²Ð²ÐµÐ´ÐµÐ½Ñ‹ Ð½ÐµÐ²ÐµÑ€Ð½Ð¾"))
				throw new CaptchaEntryInputMismatchException();
			if(PlugUtils.matcher("Ð’Ñ�Ðµ Ð¿Ð¾Ñ‚Ð¾ÐºÐ¸ Ð´Ð»Ñ� IP [0-9.]* Ð·Ð°Ð½Ñ�Ñ‚Ñ‹", contentAsString).find())
				throw new YouHaveToWaitException("Download streams for your IP exhausted", 1800);
			if(contentAsString.contains("Ð’Ð½Ð¸Ð¼Ð°Ð½Ð¸Ðµ! Ð”Ð°Ð½Ð½Ñ‹Ð¹ Ñ„Ð°Ð¹Ð» Ð±Ñ‹Ð» ÑƒÐ´Ð°Ð»ÐµÐ½"))
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