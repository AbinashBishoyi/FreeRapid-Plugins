package cz.vity.freerapid.plugins.services.uploadscenter;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class UploadsCenterFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "<a[^>]+?href\\s*?=\\s*?['\"](http[^>]+?" + Pattern.quote(httpFile.getFileName()) + ")['\"]");
        downloadLinkRegexes.add(0, "download_url=(http[^>]+?" + Pattern.quote(httpFile.getFileName()) + ")['\"]");
        downloadLinkRegexes.add(0, "href\\s*?=\\s*?['\"](http[^=]+?" + Pattern.quote(httpFile.getFileName()) + ")['\"]");
        return downloadLinkRegexes;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("id='thelink'");
        return downloadPageMarkers;
    }

}