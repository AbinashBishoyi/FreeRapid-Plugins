package cz.vity.freerapid.plugins.services.rapidshare;

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

/**
 * @author Ladislav Vitasek
 */
class RapidShareRunner {
    private final static Logger logger = Logger.getLogger(RapidShareRunner.class.getName());
    private HttpDownloadClient client;

    public void run(HttpFileDownloader downloader) throws Exception {
        HttpFile httpFile = downloader.getDownloadFile();
        client = downloader.getClient();
        final String fileURL = httpFile.getFileUrl().toString();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = client.getGetMethod(fileURL);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {
            Matcher matcher = Pattern.compile("form id=\"ff\" action=\"([^\"]*)\"", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (!matcher.find()) {
                matcher = Pattern.compile("class=\"klappbox\">((\\s|.)*?)</div>", Pattern.MULTILINE).matcher(client.getContentAsString());
                if (matcher.find()) {
                    final String error = matcher.group(1);
                    if (error.contains("illegal content") || error.contains("file has been removed") || error.contains("has removed file"))
                        throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>" + error);
                    if (error.contains("file could not be found"))
                        throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>" + error);
                    logger.warning(client.getContentAsString());
                    throw new InvalidURLOrServiceProblemException("<b>RapidShare error:</b><br>" + error);
                }
                if (client.getContentAsString().contains("has removed file"))
                    throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>The uploader has removed this file from the server.");
                if (client.getContentAsString().contains("file could not be found"))
                    throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>The file could not be found. Please check the download link.");
                if (client.getContentAsString().contains("illegal content"))
                    throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>Illegal content. File was removed.");
                if (client.getContentAsString().contains("file has been removed"))
                    throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>Due to a violation of our terms of use, the file has been removed from the server.");
                logger.warning(client.getContentAsString());
                throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
            }
            String s = matcher.group(1);
            //| 5277 KB</font>
            matcher = Pattern.compile("\\| (.*?) KB</font>", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find())
                httpFile.setFileSize(new Integer(matcher.group(1).replaceAll(" ", "")) * 1024);

            logger.info("Found File URL - " + s);
            client.setReferer(fileURL);
            final PostMethod postMethod = client.getPostMethod(s);
            postMethod.addParameter("dl.start", "Free");
            if (client.makeRequest(postMethod) == HttpStatus.SC_OK) {
                matcher = Pattern.compile("var c=([0-9]*);", Pattern.MULTILINE).matcher(client.getContentAsString());
                if (!matcher.find()) {
                    checkProblems();
                    logger.log(Level.WARNING, client.getContentAsString());
                    throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
                }
                s = matcher.group(1);
                int seconds = new Integer(s);
                matcher = Pattern.compile("form name=\"dlf\" action=\"([^\"]*)\"", Pattern.MULTILINE).matcher(client.getContentAsString());
                if (matcher.find()) {
                    s = matcher.group(1);
                    logger.info("Download URL: " + s);
                    downloader.sleep(seconds + 1);
                    if (downloader.isTerminated())
                        throw new InterruptedException();
                    httpFile.setState(DownloadState.GETTING);
                    final PostMethod method = client.getPostMethod(s);
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
                        method.abort();//really important lines!!!!!
                        method.releaseConnection();
                    }
                } else {
                    checkProblems();
                    logger.info(client.getContentAsString());
                    throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
                }
            }
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException {
        Matcher matcher;//Your IP address XXXXXX is already downloading a file.  Please wait until the download is completed.
        final String contentAsString = client.getContentAsString();
        matcher = Pattern.compile("IP address (.*?) is already", Pattern.MULTILINE).matcher(contentAsString);
        if (matcher.find()) {
            final String ip = matcher.group(1);
            throw new ServiceConnectionProblemException(String.format("<b>RapidShare error:</b><br>Your IP address %s is already downloading a file. <br>Please wait until the download is completed.", ip));
        }
        if (contentAsString.contains("Currently a lot of users")) {
            matcher = Pattern.compile("Please try again in ([0-9]*) minute", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(contentAsString);
            if (matcher.find()) {
                throw new YouHaveToWaitException("Currently a lot of users are downloading files.", Integer.parseInt(matcher.group(1)) * 60 + 20);
            }
            throw new ServiceConnectionProblemException(String.format("<b>RapidShare error:</b><br>Currently a lot of users are downloading files."));
        }
        if (contentAsString.contains("You have reached the")) {
            matcher = Pattern.compile("try again in about ([0-9]*) minute", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(contentAsString);
            if (matcher.find()) {
                throw new YouHaveToWaitException("You have reached the download-limit for free-users.", Integer.parseInt(matcher.group(1)) * 60 + 10);
            }
            throw new ServiceConnectionProblemException(String.format("<b>RapidShare error:</b><br>You have reached the download-limit for free-users."));
        }
    }

}

