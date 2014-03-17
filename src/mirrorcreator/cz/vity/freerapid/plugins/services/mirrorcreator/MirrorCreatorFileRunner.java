package cz.vity.freerapid.plugins.services.mirrorcreator;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
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
 * @author tong2shot
 * @author ntoskrnl
 */
class MirrorCreatorFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MirrorCreatorFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<div class=\"file\">\\s*<h3>(.+?)\\((.+?)\\)</h3>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name/size not found");
        }
        httpFile.setFileName(matcher.group(1));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize();
            fileURL = method.getURI().toString();
            final String uid = PlugUtils.getStringBetween(contentAsString, "/status.php?uid=", "\"");
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAjax()
                    .setAction("/status.php?uid=" + uid)
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final List<URI> list = getMirrors();
            if (list.isEmpty()) {
                throw new URLNotAvailableAnymoreException("No available mirrors");
            }
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private List<URI> getMirrors() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("<a href=\"(/redirect/.+?)\"");
        final List<URI> list = new LinkedList<URI>();
        while (matcher.find()) {
            final HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(matcher.group(1))
                    .toGetMethod();
            if (makeRedirectedRequest(method)) {
                final String url = PlugUtils.getStringBetween(getContentAsString(), "redirecturl\">", "</div>");
                try {
                    list.add(new URI(url));
                } catch (final URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
        return list;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Link disabled or is invalid")
                || contentAsString.contains("the link you have clicked is not available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    @Override
    protected String getBaseURL() {
        return "http://www.mirrorcreator.com";
    }

}