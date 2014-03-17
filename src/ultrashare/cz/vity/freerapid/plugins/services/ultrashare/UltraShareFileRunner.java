package cz.vity.freerapid.plugins.services.ultrashare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Vity
 */
class UltraShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UltraShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        Matcher matcher = PlugUtils.matcher("Download</span>\\s(.+?)\\s\\(<", content);
        if (matcher.find()) {
            final String fileName = matcher.group(1).trim(); //method trim removes white characters from both sides of string
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

            matcher = PlugUtils.matcher("\\(<i>(.+?)</i>\\)", content);
            if (matcher.find()) {
                final long size = PlugUtils.getFileSizeFromString(matcher.group(1));
                httpFile.setFileSize(size);
            } else {
                checkProblems();
                logger.warning("File size was not found\n:");
                throw new PluginImplementationException();
            }
        } else {
            checkProblems();
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            client.setReferer(fileURL);
            Matcher matcher = getMatcherAgainstContent("href=\"(.+?)\">Download This File");
            if (matcher.find()) {
                GetMethod getMethod = getGetMethod(matcher.group(1));
                if (!makeRedirectedRequest(getMethod)) {
                    throw new ServiceConnectionProblemException();
                }

                matcher = getMatcherAgainstContent("href=\"(.+?)\" class=\"download_url\">click here");
                if (matcher.find()) {
                    getMethod = getGetMethod(matcher.group(1));
                    if (!tryDownloadAndSaveFile(getMethod)) {
                        checkProblems();
                        logger.warning(getContentAsString());
                        throw new ServiceConnectionProblemException();
                    }
                }
                else throw new PluginImplementationException();
            } else throw new PluginImplementationException();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("This file doesn't exist or has been removed")) {
            throw new URLNotAvailableAnymoreException("This file doesn't exist or has been removed");
        }
    }

}
