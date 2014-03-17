package cz.vity.freerapid.plugins.services.uploadedto;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.*;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek
 */
class UploadedToRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadedToRunner.class.getName());

    public void runCheck(HttpFileDownloader downloader) throws Exception {
        super.runCheck(downloader);
        final GetMethod getMethod = client.getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkSize(client.getContentAsString());
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            final String contentAsString = client.getContentAsString();
            checkSize(contentAsString);

            Matcher matcher = PlugUtils.matcher("var secs = ([0-9]+);", contentAsString);
            if (!matcher.find()) {
                if (contentAsString.contains("is exceeded")) {
                    matcher = PlugUtils.matcher("wait ([0-9]+) minute", contentAsString);
                    if (matcher.find()) {
                        Integer waitMinutes = Integer.valueOf(matcher.group(1));
                        if (waitMinutes == 0)
                            waitMinutes = 1;
                        throw new YouHaveToWaitException("<b>Uploaded.to error:</b><br>Your Free-Traffic is exceeded!", (waitMinutes * 60));
                    }
                    throw new YouHaveToWaitException("<b>Uploaded.to error:</b><br>Your Free-Traffic is exceeded!", 60);
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

            matcher = PlugUtils.matcher("action=\"([^\"]*)\"", client.getContentAsString());
            if (matcher.find()) {
                s = matcher.group(1);
                logger.info("Found File URL - " + s);
                if (downloader.isTerminated())
                    throw new InterruptedException();
                httpFile.setState(DownloadState.GETTING);
                final GetMethod method = client.getGetMethod(s);
                //method.addParameter("mirror", "on");
                if (!tryDownload(method)) {
                    checkProblems();
                    logger.warning(client.getContentAsString());
                    throw new IOException("File input stream is empty.");
                }
            } else {
                checkProblems();
                logger.info(client.getContentAsString());
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }

        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkSize(String content) throws Exception {

        if (!content.contains("uploaded.to")) {
            logger.warning(client.getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (content.contains("File doesn")) {
            throw new URLNotAvailableAnymoreException("<b>Uploaded.to error:</b><br>File doesn't exist");
        }

        Matcher matcher = PlugUtils.matcher("([0-9.]+ .B)", content);
        if (matcher.find()) {
            final String fileSize = matcher.group(1);
            logger.info("File size " + fileSize);
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize));

        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

    }


    private void checkProblems() throws ServiceConnectionProblemException {
        Matcher matcher;//Your IP address XXXXXXX is already downloading a file.  Please wait until the download is completed.
        matcher = PlugUtils.matcher("already downloading", client.getContentAsString());
        if (matcher.find()) {
            final String ip = matcher.group(1);
            throw new ServiceConnectionProblemException(String.format("<b>Uploaded.to Error:</b><br>Your IP address %s is already downloading a file. <br>Please wait until the download is completed.", ip));
        }
        if (client.getContentAsString().indexOf("Currently a lot of users") >= 0) {
            throw new ServiceConnectionProblemException(String.format("<b>Uploaded to Error:</b><br>Currently a lot of users are downloading files."));
        }
    }

}