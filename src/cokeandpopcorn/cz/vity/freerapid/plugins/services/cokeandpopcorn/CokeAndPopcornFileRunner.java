package cz.vity.freerapid.plugins.services.cokeandpopcorn;

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
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class CokeAndPopcornFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CokeAndPopcornFileRunner.class.getName());

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
        try {
            PlugUtils.checkName(httpFile, getContentAsString(), "<meta property=\"og:title\" content=\"", "\" />");
        } catch (Exception e) {
            PlugUtils.checkName(httpFile, getContentAsString(), ">> Watch", "Online<");
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
            if (getContentAsString().contains("<div class=\"episodecontainer\">") ||
                    getContentAsString().contains("><div class=\"contain\">")) {
                List<URI> list = new LinkedList<URI>();
                final String episodes;
                if (getContentAsString().contains("<div class=\"episodecontainer\">"))
                    episodes = PlugUtils.getStringBetween(getContentAsString(), "<div class=\"episodecontainer\">", "</div>");
                else
                    episodes = PlugUtils.getStringBetween(getContentAsString(), "><div class=\"contain\">", "</div>");
                final Matcher m = PlugUtils.matcher("<a href=\"([^\"]+?)\" title", episodes);
                while (m.find()) {
                    list.add(new URI(m.group(1).trim()));
                }
                if (list.isEmpty()) throw new PluginImplementationException("No links found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                httpFile.setFileName("Link(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);
                return;
            }
            Matcher matcher = getMatcherAgainstContent("\"/js/return\\.php\", \\{\\s*hash: \"(.+?)\", episodeHash: \"(.+?)\\s*\\}");
            if (!matcher.find()) {
                throw new PluginImplementationException("Video data not found");
            }
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("/js/return.php")
                    .setParameter("hash", matcher.group(1))
                    .setParameter("episodeHash", matcher.group(2))
                    .toPostMethod();
            if (makeRedirectedRequest(method)) {
                matcher = getMatcherAgainstContent("<IFRAME SRC=\\\\\"http://vidup\\.me/embed-([^-]+)");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Video embed not found");
                }
                final URL url = new URL("http://vidup.me/" + matcher.group(1));
                httpFile.setNewURL(url);
                httpFile.setPluginID("");
                httpFile.setState(DownloadState.QUEUED);
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("the page you are looking for cannot be found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}