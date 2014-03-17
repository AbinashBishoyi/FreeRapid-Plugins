package cz.vity.freerapid.plugins.services.ifile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.io.IOException;

/**
 * @author Kajda
 */
class IFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(IFileFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

   @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();

            if (fileURL.endsWith("/")) {
                fileURL = fileURL.substring(0, fileURL.length() - 1);
            }

            final int index = fileURL.lastIndexOf("/");
            
            if (index > 0) {
                String URL = "http://ifile.it/download:dl_request?is=" + fileURL.substring(index + 1) + ",type=simple,message=ok";
                client.setReferer(URL);
                getMethod = getGetMethod(URL);

                if (!makeRedirectedRequest(getMethod)) {
                    throw new ServiceConnectionProblemException();
                }

                checkProblems();

                URL = "http://ifile.it/dl";
                client.setReferer(URL);
                getMethod = getGetMethod(URL);

                if (!makeRedirectedRequest(getMethod)) {
                    throw new ServiceConnectionProblemException();
                }

                final Matcher matcher = getMatcherAgainstContent("href=\"(.+?)\">Download<");

                if (matcher.find()) {
                    final String finalURL = matcher.group(1);
                    client.setReferer(finalURL);
                    getMethod = getGetMethod(finalURL);

                    if (!tryDownloadAndSaveFile(getMethod)) {
                        checkProblems();
                        logger.warning(getContentAsString());
                        throw new IOException("File input stream is empty");
                    }
                }
                else {
                    throw new PluginImplementationException("Download link was not found");
                }
            }
            else {
                throw new PluginImplementationException("File key was not found");
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("file removed")) {
            throw new URLNotAvailableAnymoreException("file removed, complaints received from copyright owners");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("gray;\">((?:.|\\s)+?)\\(");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

            matcher = getMatcherAgainstContent("gray;\">(?:.|\\s)+?\\((.+?)\\)");

            if (matcher.find()) {
                final long fileSize = PlugUtils.getFileSizeFromString(matcher.group(1));
                logger.info("File size " + fileSize);
                httpFile.setFileSize(fileSize);
            } else {
                checkProblems();
                logger.warning("File size was not found");
                throw new PluginImplementationException();
            }
        } else {
            checkProblems();
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}