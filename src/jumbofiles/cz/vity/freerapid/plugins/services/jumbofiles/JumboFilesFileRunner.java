package cz.vity.freerapid.plugins.services.jumbofiles;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot,birchie
 */
class JumboFilesFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new JumboFilesFileNameHandler());
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new JumboFilesFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected boolean checkDownloadPageMarker() {
        if (super.checkDownloadPageMarker()) {
            return true;
        } else {
            for (final String downloadLinkRegex : getDownloadLinkRegexes()) {
                final Matcher matcher = getMatcherAgainstContent(downloadLinkRegex);
                if (matcher.find()) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "<FORM METHOD\\s*=\\s*\"LINK\" ACTION\\s*=\\s*\"(http.+?" + Pattern.quote(httpFile.getFileName()) + ")\">");
        return downloadLinkRegexes;
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Not Found or Deleted") || contentAsString.contains("file was removed") || contentAsString.contains("File is deleted or not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("server is in maintenance mode") || contentAsString.contains("we are performing maintenance on this server")) {
            throw new PluginImplementationException("This server is in maintenance mode. Please try again later.");
        }
        // calling super.checkFileProblems() will catch "File Not Found", which is not the case
    }
}