
package cz.vity.freerapid.plugins.services.rapidshare_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.HttpFile;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * @author Ladislav Vitasek & Tomáš Procházka <to.m.p@atomsoft.cz>
 */
class RapidShareRunner {

	private final static Logger logger = Logger.getLogger(RapidShareRunner.class.getName());
	private RapidShareConfigProvider configProvider;
	private HttpDownloadClient client;

	public void run(HttpFileDownloader downloader) throws Exception {

		configProvider = new RapidShareConfigProvider();
		logger.info("Starting download in TASK " + downloader.getDownloadFile().getFileUrl());
		client = downloader.getClient();

		int i = 0;
		do {
			try {
				i++;
				tryDownload(downloader);
				break;
			} catch (BadLoginException ex) {
				configProvider.clear();
				logger.log(Level.WARNING, "Bad password or login!");
			}
		} while (i < 4);
	}

	private void tryDownload(HttpFileDownloader downloader) throws IOException, Exception {
		HttpFile httpFile = downloader.getDownloadFile();
		final String fileURL = httpFile.getFileUrl().toString();
		final GetMethod getMethod = client.getGetMethod(fileURL);

		checkLogin();

		InputStream is = client.makeFinalRequestForFile(getMethod, httpFile);
		// Redirect directly to download file.
		if (getMethod.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
			logger.info("Direct download mode");
			Header header = getMethod.getResponseHeader("location");
			getMethod.releaseConnection();
			String newUri = null;
			if (header != null) {
				newUri = header.getValue();
			}
			if (newUri != null) {
				finalDownload(newUri, downloader, 0);
			}
		} else if (getMethod.getStatusCode() == HttpStatus.SC_OK) {

			chechFileNotFound();

			if (client.getContentAsString().contains("Your Cookie has not been recognized")) {
				throw new BadLoginException();
			}

			Matcher matcher = Pattern.compile("form id=\"ff\" action=\"([^\"]*)\"", Pattern.MULTILINE).matcher(client.getContentAsString());
			if (matcher.find()) {
				String s = matcher.group(1);
				//| 5277 KB</font>
				matcher = Pattern.compile("\\| (.*?) KB</font>", Pattern.MULTILINE).matcher(client.getContentAsString());
				if (matcher.find()) {
					httpFile.setFileSize(new Integer(matcher.group(1).replaceAll(" ", "")) * 1024);
				}
				logger.info("Found File URL - " + s);
				client.setReferer(fileURL);
				final PostMethod postMethod = client.getPostMethod(s);
				postMethod.addParameter("dl.start", "PREMIUM");
				if (client.makeRequest(postMethod) == HttpStatus.SC_OK) {
					if (client.getContentAsString().contains("Your Cookie has not been recognized")) {
						throw new BadLoginException();
					}
					matcher = Pattern.compile("(http://.*?\\.rapidshare\\.com/files/.*?)\"", Pattern.MULTILINE).matcher(client.getContentAsString());
					if (matcher.find()) {
						s = matcher.group(1);
						finalDownload(s, downloader, 0);
					} else {
						checkProblems();
						logger.info(client.getContentAsString());
						throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
					}
				}
			}
		} else {
			throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
		}
	}

	private void chechFileNotFound() throws URLNotAvailableAnymoreException, InvalidURLOrServiceProblemException {
		Matcher matcher = Pattern.compile("<h1>Error.*?class=\"klappbox\">(.*?)</div>", Pattern.MULTILINE).matcher(client.getContentAsString());
		if (matcher.find()) {
			final String error = matcher.group(1);
			if (error.contains("illegal content") || error.contains("could not be found")) {
				throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>" + error);
			}
			logger.warning("RapidShare error:" + error);
			throw new InvalidURLOrServiceProblemException("<b>RapidShare error:</b><br>" + error);
		}
		if (client.getContentAsString().contains("illegal content")) {
			throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br> Illegal content. File was removed.");
		}
		if (client.getContentAsString().contains("could not be found")) {
			throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br> The file could not be found. Please check the download link.");
		}
		if (client.getContentAsString().contains("error")) {
			logger.warning(client.getContentAsString());
			throw new InvalidURLOrServiceProblemException("Unknown error");
		}
	}

	private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException {
		Matcher matcher;//Your IP address XXXXXX is already downloading a file.  Please wait until the download is completed.
		final String contentAsString = client.getContentAsString();
		matcher = Pattern.compile("IP address (.*?) is already", Pattern.MULTILINE).matcher(contentAsString);
		if (matcher.find()) {
			final String ip = matcher.group(1);
			throw new ServiceConnectionProblemException(String.format("<b>RapidShare error:</b><br>Your IP address %s is already downloading a file. <br>Please wait until the download is completed.", ip));
		}
		if (contentAsString.indexOf("Currently a lot of users") >= 0) {
			matcher = Pattern.compile("Please try again in ([0-9]*) minute", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(contentAsString);
			if (matcher.find()) {
				throw new YouHaveToWaitException("Currently a lot of users are downloading files.", Integer.parseInt(matcher.group(1)) * 60 + 20);
			}
			throw new ServiceConnectionProblemException(String.format("<b>RapidShare error:</b><br>Currently a lot of users are downloading files."));
		}
	}

	private void finalDownload(String url, HttpFileDownloader downloader, int seconds) throws Exception, IllegalArgumentException, InterruptedException {
		logger.info("Download URL: " + url);
		downloader.sleep(seconds + 1);
		if (downloader.isTerminated()) {
			throw new InterruptedException();
		}
		HttpFile httpFile = downloader.getDownloadFile();
		httpFile.setState(DownloadState.GETTING);
		final PostMethod method = client.getPostMethod(url);
		method.addParameter("mirror", "on");
		try {
			final InputStream inputStream = client.makeFinalRequestForFile(method, httpFile);
			if (inputStream != null) {
				downloader.saveToFile(inputStream);
			} else {
				checkProblems();
				throw new IOException("File input stream is empty.");
			}
		} finally {
			method.abort(); //really important lines!!!!!
			method.releaseConnection();
		}
	}

	private void checkLogin() {
		synchronized (RapidShareRunner.class) {
			String cookie = configProvider.getConfig().getCookie();
			if (cookie == null) {
				RapidShareLoginUI loginDialog = new RapidShareLoginUI();

				final SingleFrameApplication app = (SingleFrameApplication) Application.getInstance();
				app.show(loginDialog);

				do {
					Thread.yield();
				} while (loginDialog.isVisible());

				RapidShareConfig config = new RapidShareConfig(loginDialog.getLogin(), loginDialog.getPassword());
				configProvider.save(config);
				cookie = config.getCookie();
			}

			client.getHTTPClient().getState().addCookie(new Cookie("rapidshare.com", "user", cookie, "/", 86400, false));
		}
	}
}

