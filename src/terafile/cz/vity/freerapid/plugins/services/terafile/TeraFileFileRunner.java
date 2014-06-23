package cz.vity.freerapid.plugins.services.terafile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class TeraFileFileRunner extends XFileSharingRunner {

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("No such file with this filename")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        super.checkFileProblems();
    }

    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("file is available for Premium Users only")) {
            throw new NotRecoverableDownloadException("This file is only available to premium users");
        }
        super.checkDownloadProblems();
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder(final String content) throws Exception {
        return getXFSMethodBuilder(content, "method_free");
    }

    @Override
    protected boolean stepProcessFolder() throws Exception {
        if (fileURL.contains("/dir/")) {
            List<URI> list = new LinkedList<URI>();
            boolean morePages;
            do {
                morePages = false;
                final Matcher match = PlugUtils.matcher("class=\"FileManagerItems\" align=left><a target=\"_blank\" href=\"(.+?)\"", getContentAsString());
                while (match.find()) {
                    list.add(new URI(match.group(1)));
                }
                if (getContentAsString().contains(">Next")) {
                    morePages = true;
                    final HttpMethod nextMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setActionFromAHrefWhereATagContains("Next").toGetMethod();
                    if (!makeRedirectedRequest(nextMethod)) {
                        checkFileProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkFileProblems();
                    checkDownloadProblems();
                }
            } while (morePages);

            if (list.isEmpty()) throw new PluginImplementationException("No links found");
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
            httpFile.setFileName("Link(s) Extracted !");
            httpFile.setState(DownloadState.COMPLETED);
            httpFile.getProperties().put("removeCompleted", true);
            return true;
        }
        return false;
    }

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                PlugUtils.checkName(httpFile, content, "<Title>", "</Title>");
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandler() {
            @Override
            public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                final Matcher match = PlugUtils.matcher("\\((\\d+?) total\\)", content);
                if (!match.find()) throw new PluginImplementationException("No folder file count");
                httpFile.setFileSize(Long.parseLong(match.group(1)));
            }
        });
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

}