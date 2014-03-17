package cz.vity.freerapid.plugins.services.uploadedto;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek
 */
class UploadedToRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadedToRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkSize(getContentAsString());
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            final String contentAsString = getContentAsString();
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
            downloadTask.sleep(seconds + 1);

            matcher = PlugUtils.matcher("action=\"([^\"]*)\"", getContentAsString());
            if (matcher.find()) {
                s = matcher.group(1);
                logger.info("Found File URL - " + s);

                final GetMethod method = getGetMethod(s);
                //method.addParameter("mirror", "on");
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty.");
                }
            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }

        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkSize(String content) throws Exception {

        if (!content.contains("uploaded.to")) {
            logger.warning(getContentAsString());
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
        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>Uploaded.to Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>Uploaded to Error:</b><br>Currently a lot of users are downloading files."));
        }
    }

}