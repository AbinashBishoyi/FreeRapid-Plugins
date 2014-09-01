package cz.vity.freerapid.plugins.services.pururin;

import cz.vity.freerapid.plugins.exceptions.*;
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
class PururinFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PururinFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        fileURL = fileURL.replaceFirst("/thumbs/", "/gallery/");
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
        if (!fileURL.contains("/view/")) {
            Matcher match = PlugUtils.matcher("<h1.*?>(.+?)(<|Thumbnails)", content);
            if (!match.find()) throw new PluginImplementationException("File name not found");
            httpFile.setFileName(match.group(1));
            match = PlugUtils.matcher("Pages</td><td>(.+?)\\(", content);
            if (match.find())
                httpFile.setFileSize(Integer.parseInt(match.group(1).trim()));
        } else {
            Matcher match = PlugUtils.matcher("<img class=\"b\" src=\"(.+?)\"", content);
            if (!match.find()) throw new PluginImplementationException("File name not found");
            httpFile.setFileName(match.group(1).substring(match.group(1).lastIndexOf("/") + 1));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        fileURL = fileURL.replaceFirst("/gallery/", "/thumbs/");
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String content = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(content);//extract file name and size from the page
            if (!fileURL.contains("/view/")) {
                List<URI> list = new LinkedList<URI>();
                // find all links on the page
                final Matcher m = PlugUtils.matcher("<a href=\"(.*?/view/.+?)\"", getContentAsString());
                while (m.find()) {
                    list.add(new URI(getMethodBuilder().setReferer(fileURL).setAction(m.group(1).trim()).getEscapedURI()));
                }
                if (list.isEmpty()) throw new PluginImplementationException("No links found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                httpFile.setFileName("Link(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);
            } else {
                final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromTextBetween("<img class=\"b\" src=\"", "\"").toGetMethod();
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Page not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("Pururin is under maintenance")) {
            throw new YouHaveToWaitException("Pururin is under maintenance", 30 * 60);
        }
    }
}