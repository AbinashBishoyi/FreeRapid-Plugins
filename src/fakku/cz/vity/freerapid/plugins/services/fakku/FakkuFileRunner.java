package cz.vity.freerapid.plugins.services.fakku;

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

import java.net.ConnectException;
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
class FakkuFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FakkuFileRunner.class.getName());

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
        if (fileURL.contains("/videos/"))
            PlugUtils.checkName(httpFile, content, "<h1>", "</");
        else
            PlugUtils.checkName(httpFile, content, "<h1 itemprop=\"name\">", "</");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            String content = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(content);//extract file name and size from the page
            final HttpMethod dlMethod;
            if (fileURL.contains("/videos/")) {
                final String source = PlugUtils.getStringBetween(content, "source src=\"", "\"");
                httpFile.setFileName(httpFile.getFileName() + source.substring(source.lastIndexOf(".")));
                dlMethod = getGetMethod(source);
            } else {
                final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL)
                        .setActionFromAHrefWhereATagContains("Download").toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                content = getContentAsString();
                try {
                    final String link = PlugUtils.getStringBetween(content, ">fu</span> <a class=\"link\" href=\"", "\"");
                    httpFile.setFileName(link.substring(link.lastIndexOf("/") + 1));
                    dlMethod = getGetMethod(link);
                } catch (Exception e) {
                    getAlternativeLinks(content, true);
                    return;
                }
            }
            try {
                if (!tryDownloadAndSaveFile(dlMethod)) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            } catch (ConnectException c) {
                getAlternativeLinks(content, false);
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void getAlternativeLinks(String content, boolean first) throws Exception {
        List<URI> list = new LinkedList<URI>();
        final Matcher m = PlugUtils.matcher("<a class=\"link\" href=\"(.+?)\">", content);
        while (m.find()) {
            if (!m.group(1).contains("fakku.net"))
                list.add(new URI(m.group(1).trim()));
        }
        if (list.isEmpty()) {
            if (first) throw new PluginImplementationException("No links found");
            else throw new ConnectException("Connection timed out: connect");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.setFileName(httpFile.getFileName() + " - Link(s) Extracted !");
        httpFile.setState(DownloadState.COMPLETED);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Content does not exist")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}