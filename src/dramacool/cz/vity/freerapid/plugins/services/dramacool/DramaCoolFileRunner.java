package cz.vity.freerapid.plugins.services.dramacool;

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
class DramaCoolFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DramaCoolFileRunner.class.getName());
    private boolean series = false;

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
        final Matcher match = PlugUtils.matcher("<h1(.*?)>(.+?)</h1>", content);
        if (!match.find())
            throw new PluginImplementationException("File name not found");
        if (match.group(1).length() == 0) {
            httpFile.setFileName("Series: " + match.group(2).trim());
            series = true;
        } else
            httpFile.setFileName(match.group(2).trim() + ".mp4");
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
            checkNameAndSize(content);
            if (series) {
                List<URI> list = new LinkedList<URI>();
                final Matcher matchEpisodes = PlugUtils.matcher("thumbnail-body-item\">\\s*?<a href=\"(.+?)\">", content);
                while (matchEpisodes.find()) {
                    list.add(new URI(matchEpisodes.group(1)));
                }
                if (list.isEmpty()) throw new PluginImplementationException("No links found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                httpFile.setFileName("Link(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);
            } else {
                final Matcher matchVidData = PlugUtils.matcher("<iframe.+?src=\"(.+?embed.+?)\"", content);
                if (!matchVidData.find())
                    throw new PluginImplementationException("Video data not found");
                if (!makeRedirectedRequest(getGetMethod(matchVidData.group(1)))) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                final Matcher matchVidLink = PlugUtils.matcher("<source src=\"(.+?)\" type='video/(.+?)'", getContentAsString());
                if (!matchVidLink.find())
                    throw new PluginImplementationException("Video link not found");
                httpFile.setFileName(httpFile.getFileName().substring(0, httpFile.getFileName().length() - 3) + matchVidLink.group(2));

                final HttpMethod httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(matchVidLink.group(1))
                        .toGetMethod();
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
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Error 404") ||
                contentAsString.contains("requested page does not exist")) {
            throw new URLNotAvailableAnymoreException("Page not found"); //let to know user in FRD
        }
    }

}