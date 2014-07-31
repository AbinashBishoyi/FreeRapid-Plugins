package cz.vity.freerapid.plugins.services.sharesix;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u2
 */
class ShareSixFileRunner extends XFileSharingRunner {
    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, fileNameHandlers.remove(2));
        fileNameHandlers.add(0, new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                final Matcher matcher = Pattern.compile("Filename:.*?<p>(.+?)(?:\\((?:[\\s\\d\\.,]+?(?:bytes|.B|.b))\\s*\\))?</p", Pattern.DOTALL).matcher(content);
                if (!matcher.find()) {
                    throw new PluginImplementationException("File name not found");
                }
                httpFile.setFileName(matcher.group(1).trim());
            }
        });
        fileNameHandlers.add(0, new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                final Matcher matcher = Pattern.compile(">\\s*Download File ([^<>]+?)(?:\\((?:[\\s\\d\\.,]+?(?:bytes|.B|.b))\\s*\\))?</p", Pattern.DOTALL).matcher(content);
                if (!matcher.find()) {
                    throw new PluginImplementationException("File name not found");
                }
                httpFile.setFileName(matcher.group(1).trim());
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("Create Download link");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("var lnk.*?\\s*?=\\s*?['\"](http://[^'\"]+?)['\"]");
        return downloadLinkRegexes;
    }
}