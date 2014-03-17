package cz.vity.freerapid.plugins.services.nosvideo;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class NosVideOFileRunner extends XFileSharingRunner {

    @Override
    protected void setLanguageCookie() throws Exception {
        if (fileURL.contains("nosvideo.com/"))
            fileURL = fileURL.replaceFirst("nosvideo.com/\\?v", "nosupload.com/?d");
        super.setLanguageCookie();
    }

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("Please wait<.+?>\\s+?<span.*?>(\\d+?)</span>");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("<input type=button onClick=\"location.href");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("location.href='(http.+?" + Pattern.quote(httpFile.getFileName()) + ")'");
        return downloadLinkRegexes;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandler() {
            @Override
            public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                //Page does not show file size
            }
        });
        return fileSizeHandlers;
    }
}