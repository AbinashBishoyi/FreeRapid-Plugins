package cz.vity.freerapid.plugins.services.minus;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Tommy
 * @author ntoskrnl, birchie
 */
class MinusFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MinusFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        if (isPageAListing()) {
            httpFile.setFileName("List : " + PlugUtils.getStringBetween(getContentAsString(), "<title>", " - Minus</title>"));
        } else {
            PlugUtils.checkName(httpFile, getContentAsString(), "<meta name=\"title\" content=\"", " - Minus\"");
            Matcher mSize = getMatcherAgainstContent("<a title=\"(.+[KMG]?B)\" class=\"btn-action");
            if (mSize.find())
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(mSize.group(1)));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            if (isPageAListing()) {
                final List<URI> listing = new LinkedList<URI>();
                final Matcher matcher = getMatcherAgainstContent(", \"id\": \"([^\"]*)\"");
                while (matcher.find()) {  //add to list
                    listing.add(new URI("http://minus.com/l" + matcher.group(1).trim()));
                }
                // add list urls to queue
                if (listing.isEmpty()) throw new PluginImplementationException("No links found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, listing);
                httpFile.setFileName("Link(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);
            } else {
                final Matcher matcher = getMatcherAgainstContent("href=\"(http://i(?:\\d+)?\\.minus\\.com/.+?)\"");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Download link not found");
                }
                method = getGetMethod(matcher.group(1));
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("The page you requested does not exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("The folder you requested has been deleted or has expired")) {
            throw new URLNotAvailableAnymoreException("Folder not found");
        }
    }

    private boolean isPageAListing() {
        final String content = getContentAsString();
        int pos = content.indexOf("<div class=\"grid_items_container\">");
        if (0 <= content.substring(pos + 1).indexOf("<div class=\"grid_items_container\">")) {
            return true;
        }
        return false;
    }

}