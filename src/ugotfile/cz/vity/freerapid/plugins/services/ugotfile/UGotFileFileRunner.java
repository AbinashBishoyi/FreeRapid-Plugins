package cz.vity.freerapid.plugins.services.ugotfile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda+JPEXS
 * @since 0.82
 */
class UGotFileFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(UGotFileFileRunner.class.getName());
    private static final String SERVICE_WEB = "http://ugotfile.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkSeriousProblems();
            checkNameAndSize();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkNameAndSize();
            final int waitTime = PlugUtils.getWaitTimeBetween(getContentAsString(), "seconds: ", ",", TimeUnit.SECONDS);
            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(SERVICE_WEB + "/captcha?key=").toHttpMethod();

            if (makeRedirectedRequest(httpMethod)) {
                if (getContentAsString().contains("valid")) {
                    downloadTask.sleep(waitTime);
                    httpMethod = getMethodBuilder().setReferer(fileURL).setAction(SERVICE_WEB + "/file/get-file").toHttpMethod();

                    if (makeRedirectedRequest(httpMethod)) {
                        if (getContentAsString().indexOf("http://") == 0) {
                            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(getContentAsString()).toHttpMethod();

                            if (!tryDownloadAndSaveFile(httpMethod)) {
                                checkAllProblems();
                                logger.warning(getContentAsString());
                                throw new IOException("File input stream is empty");
                            }
                        } else {
                            throw new PluginImplementationException("Download link was not found");
                        }
                    } else {
                        throw new ServiceConnectionProblemException();
                    }
                } else {
                    throw new PluginImplementationException("Invalid server response");
                }
            } else {
                throw new ServiceConnectionProblemException();
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("FileId and filename mismatched or file does not exist")) {
            throw new URLNotAvailableAnymoreException("FileId and filename mismatched or file does not exist");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("<h2[^>]*>\\s*(.+)\\s*</h2>");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

            matcher = getMatcherAgainstContent("<span[^>]*>\\s*(.+B)</span>");

            if (matcher.find()) {
                final long fileSize = PlugUtils.getFileSizeFromString(matcher.group(1).replace("&nbsp;", " "));
                logger.info("File size " + fileSize);
                httpFile.setFileSize(fileSize);
            } else {
                logger.warning("File size was not found");
                throw new PluginImplementationException();
            }
        } else {
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}