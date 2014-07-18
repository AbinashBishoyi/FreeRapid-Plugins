package cz.vity.freerapid.plugins.services.xfileplayer;

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
public abstract class XFilePlayerRunner extends XFileSharingRunner {

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
        downloadPageMarkers.add("jwplayer('vplayer').setup");
        downloadPageMarkers.add("jwplayer(\"vplayer\").setup");
        downloadPageMarkers.add("jwplayer('container').setup");
        downloadPageMarkers.add("jwplayer(\"container\").setup");
        downloadPageMarkers.add("jwplayer('flvplayer').setup");
        downloadPageMarkers.add("jwplayer(\"flvplayer\").setup");
        downloadPageMarkers.add("jwplayer('mediaplayer').setup");
        downloadPageMarkers.add("jwplayer(\"mediaplayer\").setup");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "['\"]?file['\"]?\\s*?:\\s*?['\"](.+?)['\"],");
        return downloadLinkRegexes;
    }
}