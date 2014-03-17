package cz.vity.freerapid.plugins.services.microsoftdownloads;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MicrosoftDownloadsFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MicrosoftDownloadsFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();

        if (isDirect()) {
            checkDirectURL();
        } else {
            final GetMethod method = getGetMethod(fileURL);
            if (makeRedirectedRequest(method)) {
                checkProblems();
                checkNameAndSize();
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private boolean isDirect() {
        return fileURL.contains("download.microsoft.com");
    }

    private boolean isMulti() {
        return getContentAsString().contains("<a name=\"filelist\">");
    }

    private void checkDirectURL() throws ErrorDuringDownloadingException {
        if (fileURL.contains("u=http")) {
            final Matcher matcher = PlugUtils.matcher("[\\?&]u=(http.+?)(?:&.+?)?$", fileURL);
            if (!matcher.find()) throw new PluginImplementationException("Error parsing direct download link");
            try {
                fileURL = URLDecoder.decode(matcher.group(1), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                LogUtils.processException(logger, e);
            }
        }
        httpFile.setFileName(fileURL.substring(fileURL.lastIndexOf('/') + 1));
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        if (isMulti()) {
            httpFile.setFileName(PlugUtils.getStringBetween(getContentAsString(), "<h1>", "</h1>") + " (multiple files)");
        } else {
            //not checking for quickInfoName to retain multi-language support

            final Matcher name = getMatcherAgainstContent("<table.*?>.+?</td><td class=\"quickInfoValue\">(.+?)</td>");
            if (!name.find()) throw new PluginImplementationException("File name not found");
            httpFile.setFileName(name.group(1));

            //this usually works, but not in all cases
            final Matcher size = getMatcherAgainstContent("<td class=\"quickInfoValue\">(\\d+?(?:[\\.,]\\d+?)?\\s*?[KkMmGg].*?)</td>");
            if (!size.find()) logger.warning("File size not found");
            else httpFile.setFileSize(parseFileSize(size.group(1)));
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private long parseFileSize(final String stringSize) {
        //sometimes size is reported in the form "31.7 MB - 63.6 MB*"
        logger.info("Size string " + stringSize);
        try {
            // u2010-2015 are dashes. some languages use different dashes, cover all cases.
            if (!stringSize.matches(".*[-\u2010\u2011\u2012\u2013\u2014\u2015].*")) {
                return PlugUtils.getFileSizeFromString(stringSize);
            } else {
                final String[] compare = stringSize.replace("*", "").split("[-\u2010\u2011\u2012\u2013\u2014\u2015]");
                //some languages use other letters for "byte" than 'B'
                final String compare1 = compare[0].trim().replaceAll("[Kk].$", "kB").replaceAll("[Mm].$", "MB").replaceAll("[Gg].$", "GB");
                final String compare2 = compare[1].trim().replaceAll("[Kk].$", "kB").replaceAll("[Mm].$", "MB").replaceAll("[Gg].$", "GB");
                return Math.max(PlugUtils.getFileSizeFromString(compare1), PlugUtils.getFileSizeFromString(compare2));
            }
        } catch (Exception e) {
            logger.warning("Error parsing size string - " + e);
            return -1;
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        if (isDirect()) {
            checkDirectURL();
            final GetMethod method = getGetMethod(fileURL);
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            final GetMethod method = getGetMethod(fileURL);
            if (makeRedirectedRequest(method)) {
                checkProblems();
                checkNameAndSize();

                if (isMulti()) {
                    final Matcher matcher = getMatcherAgainstContent("<noscript><a href=\"(.+?)\"");
                    final List<URI> uriList = new LinkedList<URI>();
                    while (matcher.find()) {
                        final String link = "http://www.microsoft.com/downloads/" + PlugUtils.replaceEntities(matcher.group(1));
                        try {
                            uriList.add(new URI(link));
                        } catch (URISyntaxException e) {
                            LogUtils.processException(logger, e);
                        }
                    }
                    if (uriList.isEmpty()) throw new PluginImplementationException("No download links found");
                    getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
                    httpFile.getProperties().put("removeCompleted", true);
                } else {
                    final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromTextBetween("window.open('", "'").toGetMethod();
                    if (!tryDownloadAndSaveFile(httpMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Redirecting in 5 seconds") || content.contains("the page you requested cannot be found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("id=\"genuineNotValidated\"")) {
            throw new NotRecoverableDownloadException("Download needs WGA validation \u2013 not supported");
        }
        if (content.contains("<BODY><P></BODY>")) {
            throw new YouHaveToWaitException("Some server error, retrying", 10);
        }
    }

}