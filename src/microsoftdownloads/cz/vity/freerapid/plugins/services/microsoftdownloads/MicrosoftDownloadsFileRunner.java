package cz.vity.freerapid.plugins.services.microsoftdownloads;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
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
    private String LINK_TYPE = "SINGLE";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();

        if (fileURL.contains("download.microsoft.com")) {
            setDirect();
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

    private void setDirect() {
        LINK_TYPE = "DIRECT";
        httpFile.setFileName(fileURL.substring(fileURL.lastIndexOf("%2f") + 3));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();

        if (content.contains("<a name=\"filelist\">")) {
            LINK_TYPE = "MULTI";
            httpFile.setFileName(PlugUtils.getStringBetween(content, "<h1>", "</h1>") + " (multiple files)");

        } else {
            //can't check for quickInfoName to retain multi-language support

            final Matcher name = getMatcherAgainstContent("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td class=\"quickInfoName\">.+?</td><td class=\"quickInfoValue\">(.+?)</td>");
            if (!name.find()) throw new PluginImplementationException("File name not found");
            httpFile.setFileName(name.group(1));

            //this usually works, but not in all cases
            final Matcher size = getMatcherAgainstContent("<td class=\"quickInfoValue\">((\\d+?(\\.\\d+?)?)( .B)(.*?))</td>");
            if (!size.find()) logger.warning("File size not found");
            else httpFile.setFileSize(parseFileSize(size.group(1)));
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private long parseFileSize(final String stringSize) {
        //sometimes size is reported in the form "31.7 MB - 63.6 MB*"
        logger.info("Size string " + stringSize);
        if (!stringSize.contains("-")) {
            return PlugUtils.getFileSizeFromString(stringSize);
        } else {
            try {
                final String[] compare = stringSize.replace("*", "").split("-");
                return Math.max(PlugUtils.getFileSizeFromString(compare[0]), PlugUtils.getFileSizeFromString(compare[1]));
            } catch (Exception e) {
                logger.warning("Error parsing size string - " + e);
                return 0;
            }
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        if (fileURL.contains("download.microsoft.com")) {
            setDirect();
            final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                throw new PluginImplementationException();
            }

        } else {
            final GetMethod method = getGetMethod(fileURL);
            if (makeRedirectedRequest(method)) {
                checkProblems();
                checkNameAndSize();

                if (LINK_TYPE.equals("MULTI")) {
                    final Matcher matcher = getMatcherAgainstContent("<noscript><a href=\"(.+?)\"");
                    int start = 0;
                    final List<URI> uriList = new LinkedList<URI>();
                    while (matcher.find(start)) {
                        String link = "http://www.microsoft.com/downloads/" + PlugUtils.replaceEntities(matcher.group(1));
                        try {
                            uriList.add(new URI(link));
                        } catch (URISyntaxException e) {
                            LogUtils.processException(logger, e);
                        }
                        start = matcher.end();
                    }
                    getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);

                } else {
                    final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromTextBetween("window.open('", "'").toGetMethod();
                    if (!tryDownloadAndSaveFile(httpMethod)) {
                        throw new PluginImplementationException();
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
        if (content.contains("Redirecting in 5 seconds")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("WhyValidate")) {
            throw new PluginImplementationException("Download needs WGA validation - not supported");
        }
        if (content.contains("<BODY><P></BODY>")) {
            throw new YouHaveToWaitException("Some server error, retrying", 10);
        }
    }

}