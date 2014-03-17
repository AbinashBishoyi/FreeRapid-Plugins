package cz.vity.freerapid.plugins.services.divshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Vity, ntoskrnl
 */
class DivshareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DivshareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (fileURL.contains("/download/")) {
                checkNameAndSize();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        Matcher matcher = getMatcherAgainstContent("<title>(?:.+?\\()?(.+?)\\)? \\- DivShare</title>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(matcher.group(1));
        final String code = PlugUtils.getStringBetween(getContentAsString(), "var currentFileCode = '", "';");
        final HttpMethod method = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("/scripts/ajax/v5/fileStats.php")
                .setParameter("code", code)
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        matcher = getMatcherAgainstContent("<b>File Size:</b>([^<>]+?)<span class=\"tiny\">([^<>]+?)</span>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1) + matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();

            if (fileURL.contains("/folder/")) {
                parseWebsite("<div class=\"folder_file_list\"[^<>]+?><a href=\"/download/(.+?)\" title");

            } else if (fileURL.contains("/playlist/")) {
                final String data = PlugUtils.getStringBetween(getContentAsString(), "audio_embed?data=", "&");
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction("/embed/audio_embed_xml.php")
                        .setParameter("data", data)
                        .toGetMethod();
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
                parseWebsite("<sound[^<>]+?file_id=\"(.+?)\"");

            } else if (fileURL.contains("/gallery/") || fileURL.contains("/slideshow/")) {
                final String id = PlugUtils.getStringBetween(getContentAsString(), "<a href=\"/download/", "\"");
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction("/embed/slideshow/" + id)
                        .toGetMethod();
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
                parseWebsite("<img src=\"http://www\\.divshare\\.com/img/display/(.+?)\"");

            } else if (fileURL.contains("/download/")) {
                checkNameAndSize();
                final String launchURL = fileURL.replace("/download/", "/download/launch/");
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(launchURL)
                        .toGetMethod();
                if (makeRedirectedRequest(method)) {
                    method = getMethodBuilder()
                            .setReferer(launchURL)
                            .setActionFromAHrefWhereATagContains("ownload")
                            .toGetMethod();
                    setFileStreamContentTypes("text/plain");
                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                } else {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            } else {
                throw new InvalidURLOrServiceProblemException("Could not determine content type - Invalid URL?");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("we couldn't find this file") || content.contains("<h1>Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("is not available to free users in China")) {
            throw new URLNotAvailableAnymoreException("This file is not available to free users in China and Southeast Asia");
        }
        if (content.contains("download limit")) { //TODO, I haven't got this error yet
            throw new URLNotAvailableAnymoreException("The uploader's monthly bandwidth has been exceeded. Try again next month.");
        }
        if (content.contains("this account has not been confirmed")) {
            throw new URLNotAvailableAnymoreException("The account this file has been uploaded from has not been confirmed");
        }
        if (content.contains("This file is secured")) {
            throw new URLNotAvailableAnymoreException("This file is secured. Please contact the uploader to gain access.");
        }
    }

    private void parseWebsite(final String regexp) throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent(regexp);
        final List<URI> list = new LinkedList<URI>();
        while (matcher.find()) {
            final String link = "http://www.divshare.com/download/" + matcher.group(1);
            try {
                list.add(new URI(link));
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        httpFile.getProperties().put("removeCompleted", true);
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
    }

}