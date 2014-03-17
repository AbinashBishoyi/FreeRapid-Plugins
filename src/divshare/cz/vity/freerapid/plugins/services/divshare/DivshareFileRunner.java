package cz.vity.freerapid.plugins.services.divshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
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
 * @author Vity, ntoskrnl
 */
class DivshareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DivshareFileRunner.class.getName());
    private String CONTENT_TYPE;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (fileURL.contains("/download/")) checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher name = getMatcherAgainstContent("<div class=\"file_name\">(?:\\s*?<img src=\"[^<>\"]+?\" valign=\"absmiddle\">)?\\s*?([^\\s][^<>]+?[^\\s])\\s*?</div>");
        if (!name.find())
            logger.warning("File name not found");
        else
            httpFile.setFileName(name.group(1));

        final Matcher size = getMatcherAgainstContent("<b>File Size:</b>([^<>]+?)<span class=\"tiny\">([^<>]+?)</span>");
        if (!size.find())
            logger.warning("File size not found");
        else
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(size.group(1) + size.group(2)));

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();

            if (fileURL.contains("/folder/")) {
                CONTENT_TYPE = "folder";
                final Matcher matcher = getMatcherAgainstContent("<h3 style=\"[^<>\"]+?\">(.+?)<small");
                if (matcher.find()) httpFile.setFileName(matcher.group(1) + " (folder)");
                parseWebsite("<div class=\"folder_file_list\"[^<>]+?><a href=\"/download/(.+?)\" title");

            } else if (fileURL.contains("/playlist/")) {
                CONTENT_TYPE = "playlist";
                final HttpMethod getXML = getMethodBuilder().setReferer(fileURL).setAction("http://www.divshare.com/embed/playlist/" + PlugUtils.getStringBetween(getContentAsString(), "myId=", "\"")).toGetMethod();
                if (!makeRedirectedRequest(getXML)) throw new ServiceConnectionProblemException();
                final Matcher matcher = getMatcherAgainstContent("<playlist title=\"(.+?)\">");
                if (matcher.find()) httpFile.setFileName(matcher.group(1) + " (playlist)");
                parseWebsite("<sound[^<>]+?file_id=\"(.+?)\"");

            } else if (fileURL.contains("/gallery/") || fileURL.contains("/slideshow/")) {
                CONTENT_TYPE = "gallery";
                final HttpMethod getXML = getMethodBuilder().setReferer(fileURL).setAction("http://www.divshare.com/embed/slideshow/" + PlugUtils.getStringBetween(getContentAsString(), "<a href=\"/download/", "\"")).toGetMethod();
                if (!makeRedirectedRequest(getXML)) throw new ServiceConnectionProblemException();
                final Matcher matcher = getMatcherAgainstContent("<album title=\"(.+?)\">");
                if (matcher.find()) httpFile.setFileName(matcher.group(1) + " (gallery)");
                parseWebsite("<img src=\"http://www\\.divshare\\.com/img/display/(.+?)\"");

            } else if (fileURL.contains("/download/")) {

                CONTENT_TYPE = "regular";
                checkNameAndSize();

                final String launchURL = fileURL.replace("/download/", "/download/launch/");

                HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(launchURL).toGetMethod();
                if (makeRedirectedRequest(httpMethod)) {
                    httpMethod = getMethodBuilder().setReferer(launchURL).setActionFromAHrefWhereATagContains("click here").toGetMethod();
                    client.getHTTPClient().getParams().setParameter("noContentTypeInHeader", true);//accept everything
                    if (!tryDownloadAndSaveFile(httpMethod)) {
                        logger.warning(getContentAsString());
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                } else throw new ServiceConnectionProblemException();

            } else {
                logger.warning(getContentAsString());
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

    private void parseWebsite(final String regexp) {
        final Matcher matcher = getMatcherAgainstContent(regexp);
        int start = 0;
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find(start)) {
            String link = "http://www.divshare.com/download/" + matcher.group(1);
            try {
                uriList.add(new URI(link));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
            start = matcher.end();
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

    @Override
    protected String getBaseURL() {
        return "http://divshare.com";
    }

}