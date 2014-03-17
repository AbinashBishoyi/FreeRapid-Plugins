package cz.vity.freerapid.plugins.services.shareplace;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

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
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
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
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            final String action = PlugUtils.getStringBetween(getContentAsString(), "var beer = '", "';")
                    .replace("vvvvvvvvv", "")
                    .replace("lllllllll", "")
                    .replace("teletubbies", "")
                    .replace("%3A", ":").replace("%2F", "/").replace("%3F", "?").replace("%3D", "=").replace("%26", "&")
                    .substring(PlugUtils.getNumberBetween(getContentAsString(), "substring(", ")"));

            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(action).toGetMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new PluginImplementationException("Error downloading file");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Your requested file is not found") || content.contains("Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("You have got max allowed download sessions from the same IP")) {
            throw new YouHaveToWaitException("You have got max allowed download sessions from the same IP", 2 * 60);
        }
    }

}