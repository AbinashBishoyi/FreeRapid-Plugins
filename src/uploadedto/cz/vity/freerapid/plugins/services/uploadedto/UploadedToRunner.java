package cz.vity.freerapid.plugins.services.uploadedto;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.HttpFile;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek
 */
class UploadedToRunner {
    private final static Logger logger = Logger.getLogger(UploadedToRunner.class.getName());
    private HttpDownloadClient client;

    public void run(HttpFileDownloader downloader) throws Exception {
        HttpFile httpFile = downloader.getDownloadFile();
        client = downloader.getClient();
        final String fileURL = httpFile.getFileUrl().toString();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {
            final String contentAsString = client.getContentAsString();
            Matcher matcher = Pattern.compile("var secs = ([0-9]+);", Pattern.MULTILINE).matcher(contentAsString);
            if (!matcher.find()) {
                if (contentAsString.contains("is exceeded")) {
                    matcher = Pattern.compile("wait ([0-9]+) minute", Pattern.MULTILINE).matcher(contentAsString);
                    if (matcher.find()) {
                        Integer waitMinutes = Integer.valueOf(matcher.group(1));
                        if (waitMinutes == 0)
                            waitMinutes = 1;
                        throw new YouHaveToWaitException("<b>Uploaded.to error:</b><br>Your Free-Traffic is exceeded!", (waitMinutes * 60));
                    }
                    throw new InvalidURLOrServiceProblemException("<b>Uploaded.to error:</b><br>Your Free-Traffic is exceeded!");
                } else if (contentAsString.contains("File doesn")) {
                    throw new URLNotAvailableAnymoreException("<b>Uploaded.to error:</b><br>File doesn't exist");
                }
                throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
            }
            String s = matcher.group(1);
            int seconds = new Integer(s);
            downloader.sleep(seconds + 1);
            if (downloader.isTerminated())
                throw new InterruptedException();

            //| 5277 KB</font>
//            matcher = Pattern.compile("\\| (.*?) KB</font>", Pattern.MULTILINE).matcher(client.getContentAsString());
//            if (matcher.find())
//                httpFile.setFileSize(new Integer(matcher.group(1).replaceAll(" ", "")) * 1024);


            matcher = Pattern.compile("action=\"([^\"]*)\"", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find()) {
                s = matcher.group(1);
                logger.info("Found File URL - " + s);
                if (downloader.isTerminated())
                    throw new InterruptedException();
                httpFile.setState(DownloadState.GETTING);
                final GetMethod method = client.getGetMethod(s);
                //method.addParameter("mirror", "on");
                try {
                    final InputStream inputStream = client.makeFinalRequestForFile(method, httpFile);
                    if (inputStream != null) {
                        downloader.saveToFile(inputStream);
                    } else {
                        checkProblems();
                        logger.warning(client.getContentAsString());
                        throw new IOException("File input stream is empty.");
                    }

                } finally {
                    method.abort();
                    method.releaseConnection();
                }
            } else {
                checkProblems();
                logger.info(client.getContentAsString());
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }

        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkProblems() throws ServiceConnectionProblemException {
        Matcher matcher;//Your IP address XXXXXXX is already downloading a file.  Please wait until the download is completed.
        matcher = Pattern.compile("already downloading", Pattern.MULTILINE).matcher(client.getContentAsString());
        if (matcher.find()) {
            final String ip = matcher.group(1);
            throw new ServiceConnectionProblemException(String.format("<b>Uploaded.to Error:</b><br>Your IP address %s is already downloading a file. <br>Please wait until the download is completed.", ip));
        }
        if (client.getContentAsString().indexOf("Currently a lot of users") >= 0) {
            throw new ServiceConnectionProblemException(String.format("<b>Uploaded to Error:</b><br>Currently a lot of users are downloading files."));
        }
    }

}