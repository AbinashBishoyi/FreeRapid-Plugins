package cz.vity.freerapid.plugins.services.mirrorcreator;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class MirrorCreatorFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MirrorCreatorFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final String filenameregexStr = "<div class=\"file\">\\s*<h3>(.+?)\\(.*?</h3>";
        final String filesizeregexStr = "<div class=\"file\">\\s*<h3>.*?\\((.+?)\\)</h3>";
        final Matcher filenameMatcher = getMatcherAgainstContent(filenameregexStr);
        final Matcher filesizeMatcher = getMatcherAgainstContent(filesizeregexStr);
        if (filenameMatcher.find()) {
            final String fileName = filenameMatcher.group(1).trim();
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);
        } else {
            throw new PluginImplementationException("File name not found");
        }
        if (filesizeMatcher.find()) {
            final String fileSize = filesizeMatcher.group(1);
            logger.info("File size " + fileSize);
            final long size = PlugUtils.getFileSizeFromString(filesizeMatcher.group(1));
            httpFile.setFileSize(size);
        } else {
            throw new PluginImplementationException("File size not found");
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

            fileURL = method.getURI().toString();
            logger.info("fileURL : " + fileURL);
            final String uid = PlugUtils.getStringBetween(contentAsString, "/status.php?uid=", "\",");
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://www.mirrorcreator.com/status.php?uid=" + uid)
                    .toGetMethod();
            httpMethod.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            contentAsString = getContentAsString();
            logger.info("ajax response : " + contentAsString);
            final List<URL> urlList = getMirrors(uid);
            if (urlList.isEmpty())
                throw new URLNotAvailableAnymoreException("No available mirrors");
            getPluginService().getPluginContext().getQueueSupport().addLinkToQueueUsingPriority(httpFile, urlList);

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }


    private List<URL> getMirrors(String uid) throws Exception {
        final String regexPattern = "<td class=\"host\">.+?<a.+?href=\"/redirect/" + uid + "/(\\d+)\"";
        final Matcher matcher = Pattern.compile(regexPattern, Pattern.MULTILINE + Pattern.DOTALL).matcher(getContentAsString());
        final List<URL> urlList = new LinkedList<URL>();
        while (matcher.find()) {
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://www.mirrorcreator.com/redirect/" + uid + "/" + matcher.group(1))
                    .toGetMethod();
            if (makeRequest(httpMethod)) {
                final String mirrorURL = PlugUtils.getStringBetween(getContentAsString(), "redirecturl\">", "</div>");
                urlList.add(new URL(mirrorURL));
            }
        }
        return urlList;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Link disabled or is invalid") || contentAsString.contains("the link you have clicked is not available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}