package cz.vity.freerapid.plugins.services.shareplace;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class SharePlaceFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SharePlaceFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(getFrameURL()).toGetMethod();
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getFrameURL() {
        final String frameURL = fileURL.replace(".com/?", ".com/index1.php?a=");
        logger.info("Frame URL " + frameURL);
        return frameURL;
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher name = getMatcherAgainstContent("Filename:</font></b>\\s*?(.+?)<b>");
        if (!name.find()) throw new PluginImplementationException("File name not found");
        httpFile.setFileName(name.group(1));

        final Matcher size = getMatcherAgainstContent("Filesize:</font></b>\\s*?(.+?)<b>");
        if (!size.find()) throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size.group(1)));

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(getFrameURL()).toGetMethod();
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            final String action = URLDecoder.decode(
                    PlugUtils.getStringBetween(getContentAsString(), "var beer = '", "';")
                            .replace("vvvvvvvvv", "")
                            .replace("lllllllll", "")
                            .replace("teletubbies", ""), "UTF-8")
                    .substring(PlugUtils.getNumberBetween(getContentAsString(), "substring(", ")"));

            logger.info("Download URL " + action);

            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(action).toGetMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Your requested file is not found") || content.contains("<H1>Not Found</H1>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("You have got max allowed download sessions from the same IP")) {
            throw new YouHaveToWaitException("You have got max allowed download sessions from the same IP", 2 * 60);
        }
    }

}