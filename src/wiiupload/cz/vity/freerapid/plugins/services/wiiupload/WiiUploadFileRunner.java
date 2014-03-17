package cz.vity.freerapid.plugins.services.wiiupload;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.net.URLEncoder;

/**
 * @author Kajda
 */
class WiiUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(WiiUploadFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(encodeURL(fileURL));

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
        fileURL = encodeURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();

            Matcher matcher = getMatcherAgainstContent("href=\"(.+?)\">Download This File");

            if (matcher.find()) {
                final String URL = matcher.group(1);
                client.setReferer(URL);
                getMethod = getGetMethod(encodeURL(matcher.group(1)));

                if (!makeRedirectedRequest(getMethod)) {
                    throw new ServiceConnectionProblemException();
                }

                checkProblems();

                matcher = getMatcherAgainstContent("href=\"(.+?)\" class=\"download_url\">click here");

                if (matcher.find()) {
                    String finalURL = matcher.group(1);
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
            } else {
                throw new PluginImplementationException();
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("This file doesn't exist or has been removed")) {
            throw new URLNotAvailableAnymoreException("This file doesn't exist or has been removed");
        }

        if (contentAsString.contains("All downloading slots are currently filled")) {
            throw new YouHaveToWaitException("All downloading slots are currently filled", 60);
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("relative;\">\\s*(.+?) \\(<i");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

            matcher = getMatcherAgainstContent("\\(<i>(.+?)</i>\\)");

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
    
    private String encodeURL(String string) throws UnsupportedEncodingException {
        Matcher matcher = PlugUtils.matcher("(.*/)([^/]*)$", string);

        if (matcher.find()) {
            return matcher.group(1) + URLEncoder.encode(matcher.group(2), "UTF-8");
        }

        return string;
    }
}