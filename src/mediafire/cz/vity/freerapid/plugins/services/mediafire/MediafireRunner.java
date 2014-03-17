package cz.vity.freerapid.plugins.services.mediafire;

import cz.vity.freerapid.plugins.exceptions.*;
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
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
 */
public class MediafireRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MediafireRunner.class.getName());

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
        if (!isList()) {
            final String content = getContentAsString();
            PlugUtils.checkName(httpFile, content, "<div class=\"download_file_title\">", "</div>");
            if (!isPassworded()) {
                PlugUtils.checkFileSize(httpFile, content, ">(", ")<");
            }
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("The key you provided for file download")
                || content.contains("How can MediaFire help you?")
                || content.contains("File Removed for Violation")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (isList()) {
                runList();
                return;
            }
            checkNameAndSize();
            if (isPassworded()) {
                stepPassword();
                checkNameAndSize();
            }
            final Matcher matcher = getMatcherAgainstContent("(<div class=\"download_link\".+?</div>)");
            if (!matcher.find()) {
                throw new PluginImplementationException("Download link not found");
            }
            method = getMethodBuilder(matcher.group(1)).setActionFromAHrefWhereATagContains("").toGetMethod();
            setFileStreamContentTypes("text/plain");
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void runList() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("src=\"(/js/myfiles.php[^\"]+?)\"");
        if (!matcher.find()) throw new PluginImplementationException("URL to list not found");
        final HttpMethod listMethod = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1)).toGetMethod();

        if (makeRedirectedRequest(listMethod)) {
            parseList();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void parseList() {
        final Matcher matcher = getMatcherAgainstContent("oe\\[[0-9]+\\]=Array\\('([^']+?)'");
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find()) {
            final String link = "http://www.mediafire.com/download.php?" + matcher.group(1);
            try {
                uriList.add(new URI(link));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

    private boolean isList() {
        return (fileURL.contains("?sharekey="));
    }

    private boolean isPassworded() {
        return getContentAsString().contains("\"form_password\"");
    }

    private void stepPassword() throws Exception {
        while (isPassworded()) {
            final HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormByName("form_password", true)
                    .setAndEncodeParameter("downloadp", getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private String getPassword() throws Exception {
        final String password = getDialogSupport().askForPassword("MediaFire");
        if (password == null) {
            throw new NotRecoverableDownloadException("This file is secured with a password");
        } else {
            return password;
        }
    }

}
