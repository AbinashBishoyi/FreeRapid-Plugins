package cz.vity.freerapid.plugins.services.nekaka;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class NekakaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NekakaFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {

        if (fileURL.contains("nekaka.com/d")) {
            PlugUtils.checkName(httpFile, content, "<nobr>", "</nobr>");
            final Matcher match = PlugUtils.matcher("<span id=\"size\"[^>]+?>\\s+?(.+?)\\s+?</span>", content);
            if (!match.find())
                throw new PluginImplementationException("File size not found");
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1)));
        } else if (fileURL.contains("nekaka.com/f")) {
            httpFile.setFileName("LIST> " + PlugUtils.getStringBetween(content, "<title>NEKAKA - ", "</title>"));
            httpFile.setFileSize(0);
        } else {
            throw new PluginImplementationException("Link not supported");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String content = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(content);//extract file name and size from the page

            if (fileURL.contains("nekaka.com/d")) {
                final int wait = PlugUtils.getNumberBetween(content, "<span id=\"waittime\">", "</span>");
                downloadTask.sleep(wait + 1);
                final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL)
                        .setActionFromFormWhereTagContains("Download", true).toGetMethod();
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            } else if (fileURL.contains("nekaka.com/f")) {
                final List<URI> listing = new LinkedList<URI>();
                final Matcher match = PlugUtils.matcher(">(https?://(www\\.)?nekaka\\.com/d.+?)<", content);
                while (match.find()) {  //add to list
                    listing.add(new URI(match.group(1).trim()));
                }
                // add list urls to queue
                if (listing.isEmpty()) throw new PluginImplementationException("No links found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, listing);
                httpFile.setFileName("Link(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file does not exist") || contentAsString.contains("404 Page Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}