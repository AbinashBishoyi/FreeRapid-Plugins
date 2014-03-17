package cz.vity.freerapid.plugins.services.toshared;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Tiago Hillebrandt <tiagohillebrandt@gmail.com>
 */
class ToSharedRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ToSharedRunner.class.getName());
    private String baseURL;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();

        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRequest(getMethod)) {
            this.checkNameAndSize(getContentAsString());
            this.checkForProblems(getContentAsString());
        } else {
            throw new PluginImplementationException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();

        this.logger.info("Starting download is TASK " + fileURL);

        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRequest(getMethod)) {
            this.getStartDownloadURL(getContentAsString());

            this.checkNameAndSize(getContentAsString());
            this.checkForProblems(getContentAsString());

            if (getContentAsString().contains("<a href=\"javascript:startDownload()\" class=\"dsumm\">click here</a>")) {
                final GetMethod method = getGetMethod(this.baseURL);

                if (!tryDownloadAndSaveFile(method)) {
                    this.checkForProblems(getContentAsString());

                    this.logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty.");
                }
            } else {
                this.logger.info(getContentAsString());
                throw new PluginImplementationException();
            }
        } else {
            throw new PluginImplementationException();
        }
    }

    public void getStartDownloadURL(String content) throws Exception {
        /* gets the download link */
        Matcher matcher = PlugUtils.matcher("window.location = \"(.*)\";", content);

        if (matcher.find()) {
            this.baseURL = matcher.group(1);
        }
    }

    public void checkNameAndSize(String content) throws Exception {
        /* verifies the url from service */
        if ((!content.contains("2shared.com")) || (content.contains("The file link that you requested is not valid."))) {
            this.logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }

        /* gets the file name */
        Matcher matcher = PlugUtils.matcher("download ([^\"]+)</title>", content);

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();

            this.logger.info("File name " + fileName);
            httpFile.setFileName(fileName.trim());
        } else {
            this.logger.warning("File name was not found" + content);
        }

        /* gets the file size */
        matcher = PlugUtils.matcher("(([0-9,.]* .B &nbsp; &nbsp;))", content);

        if (matcher.find()) {
            final String fileSize = (matcher.group(1)).replaceAll(",", "");

            this.logger.info("File size " + fileSize);
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize));
        } else {
            this.logger.warning("File size was not found" + content);
        }

        /* file was checked and exists */
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    public void checkForProblems(String content) throws ServiceConnectionProblemException {
        if (content.contains("User downloading session limit is reached.")) {
            throw new ServiceConnectionProblemException("Sorry, your IP address is already downloading a file or your session limit is reached! Try again in few minutes.");
        }
    }
}
