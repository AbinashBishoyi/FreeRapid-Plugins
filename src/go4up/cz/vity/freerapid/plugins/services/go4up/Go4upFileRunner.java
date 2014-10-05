package cz.vity.freerapid.plugins.services.go4up;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
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
 * @author birchie
 */
class Go4upFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Go4upFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            String sFileName = ".";
            if (getContentAsString().contains("<title>Download"))
                sFileName = " For: " + PlugUtils.getStringBetween(getContentAsString(), "<title>Download ", "<");
            httpFile.setFileName("Extract Link(s)" + sFileName);
            PlugUtils.checkFileSize(httpFile, getContentAsString(), " (", ")<");
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final List<URI> list = new LinkedList<URI>();
            if (fileURL.contains("/rd/"))   // single redirect url link
                processLink(fileURL, list);
            else {   // get multiple redirect url links
                final HttpMethod hostsMethod = getMethodBuilder()
                        .setActionFromTextBetween("url: \"", "\"")
                        .setReferer(fileURL).toGetMethod();
                if (!makeRedirectedRequest(hostsMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                final Matcher matcher = getMatcherAgainstContent(">(http://go4up.com/rd/[^/]+?/[\\w\\d]{1,5})</a>");
                while (matcher.find()) {
                    processLink(matcher.group(1), list);
                }
            }
            // add urls to queue
            if (list.isEmpty()) throw new PluginImplementationException("No links found");
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
            httpFile.setFileName("Link(s) Extracted !");
            httpFile.setState(DownloadState.COMPLETED);
            httpFile.getProperties().put("removeCompleted", true);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void processLink(String Go4Up_rd_Link, List<URI> listing) throws Exception {
        if (!Go4Up_rd_Link.contains("/rd/")) {
            listing.add(new URI(Go4Up_rd_Link.trim()));
        } else {
            try {//process redirection link to get final url
                final GetMethod submethod = getGetMethod(Go4Up_rd_Link);
                int httpStatus = client.makeRequest(submethod, false);
                if (httpStatus / 100 == 3) {
                    listing.add(new URI(submethod.getResponseHeader("Location").getValue()));
                } else if (httpStatus == 200) {
                    String subContent = getContentAsString().replace("\n", "").replace("\r", "");
                    if (subContent.contains("content=\"0;url=")) {
                        String strNewUrl = PlugUtils.getStringBetween(subContent, "content=\"0;url=", "\">");
                        listing.add(new URI(strNewUrl));
                    } else if (subContent.contains("window.location =")) {
                        String strNewUrl = PlugUtils.getStringBetween(subContent, "window.location = \"", "\"");
                        listing.add(new URI(strNewUrl));
                    } else {
                        final Matcher match = PlugUtils.matcher("id=\"linklist\">\\s*?<center>\\s*?.*?\\s*?<b><a href=\"(.+?)\">", getContentAsString());
                        try {
                            if (match.find())
                                listing.add(new URI(match.group(1)));
                        } catch (Exception ee) {/* ignore invalid links */}
                    }
                }
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
                throw new ServiceConnectionProblemException("Error retrieving link");
            }
        }
    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("download does not exist") || contentAsString.contains("not found on this server")
                || contentAsString.contains("Page Not Found") || contentAsString.contains("page you requested was not found")
                || contentAsString.contains("page you are looking for cannot be found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}