package cz.vity.freerapid.plugins.services.played;

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
class PlayedFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("jwplayer(\"flvplayer\").setup(");
        downloadPageMarkers.add("jwplayer(\"mediaplayer\").setup(");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("file\\s*?:\\s*?['\"](.+?)['\"],");
        return downloadLinkRegexes;
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder(final String content) throws Exception {
        final MethodBuilder methodBuilder = getMethodBuilder(content)
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("download1", true)                 //####
                .setAction(fileURL)
                .setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        if ((methodBuilder.getParameters().get("method_free") != null) && (!methodBuilder.getParameters().get("method_free").isEmpty())) {
            methodBuilder.removeParameter("method_premium");
        }
        return methodBuilder;
    }
}