package cz.vity.freerapid.plugins.services.exoshare;

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
class ExoShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ExoShareFileRunner.class.getName());

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
        httpFile.setFileName("Get Link(s):: " + PlugUtils.getStringBetween(content, "Name :", "<br"));
        PlugUtils.checkFileSize(httpFile, content, "Size :", "<br");
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

            final HttpMethod httpMethod = getMethodBuilder()
                    .setActionFromTextBetween("ajaxRequest.open(\"GET\", \"", "\",")
                    .setAjax()
                    .setReferer(fileURL)
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                throw new PluginImplementationException();
            }
            checkProblems();
            final Matcher match = PlugUtils.matcher("<a href=\"(.+?)\" target=\"_blank\">", getContentAsString());
            List<URI> list = new LinkedList<URI>();
            while (match.find()) {
                list.add(new URI(match.group(1).trim()));
            }
            if (list.isEmpty()) throw new PluginImplementationException("No link(s) found");
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(this.httpFile, list);
            this.httpFile.setFileName("Link(s) Extracted !");
            this.httpFile.setState(DownloadState.COMPLETED);
            this.httpFile.getProperties().put("removeCompleted", true);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Error File not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}