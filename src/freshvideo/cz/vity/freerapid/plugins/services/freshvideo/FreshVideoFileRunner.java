package cz.vity.freerapid.plugins.services.freshvideo;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FreshVideoFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder(final String content) throws Exception {
        return getXFSMethodBuilder(content, "download1");
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("jwplayer(\"vplayer\").setup");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "file\\s*?:\\s*?['\"](.+?)['\"]");
        return downloadLinkRegexes;
    }

    @Override
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        final String link = super.getDownloadLinkFromRegexes();
        final String ext = link.substring(link.lastIndexOf("."));
        if (!httpFile.getFileName().matches(".+?" + ext))
            httpFile.setFileName(httpFile.getFileName() + ext);
        return link;
    }
}