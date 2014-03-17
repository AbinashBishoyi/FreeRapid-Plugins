package cz.vity.freerapid.plugins.services.mediafire;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
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
        if (!isFolder()) {
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
            if (isFolder()) {
                parseFolder();
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

    private boolean isFolder() {
        return getContentAsString().contains("<body class=\"myfiles\">");
    }

    private void parseFolder() throws Exception {
        final String id = fileURL.substring(fileURL.indexOf('?') + 1);
        final List<FolderItem> list = new LinkedList<FolderItem>();
        if (id.contains(",")) {
            for (final String s : id.split(",")) {
                list.add(new FolderItem(s, null));
            }
        } else {
            final HttpMethod method = getMethodBuilder()
                    .setAction("http://www.mediafire.com/api/folder/get_info.php")
                    .setParameter("recursive", "yes")
                    .setParameter("content_filter", "files")
                    .setParameter("folder_key", id)
                    .setParameter("response_format", "json")
                    .setParameter("version", "1")
                    .toGetMethod();
            setFileStreamContentTypes(new String[0], new String[]{"application/json"});
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final Matcher matcher = getMatcherAgainstContent("\"quickkey\":\"(.+?)\",\"filename\":\"(.+?)\"");
            while (matcher.find()) {
                list.add(new FolderItem(matcher.group(1), matcher.group(2)));
            }
            Collections.sort(list);
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        final List<URI> uriList = new LinkedList<URI>();
        for (final FolderItem item : list) {
            try {
                uriList.add(new URI(item.getFileUrl()));
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        httpFile.getProperties().put("removeCompleted", true);
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

    private static class FolderItem implements Comparable<FolderItem> {
        private final String fileId;
        private final String fileName;

        public FolderItem(final String fileId, final String fileName) {
            this.fileId = fileId;
            this.fileName = fileName;
        }

        public String getFileUrl() {
            return "http://www.mediafire.com/?" + fileId;
        }

        @Override
        public int compareTo(final FolderItem that) {
            return this.fileName.compareTo(that.fileName);
        }
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
