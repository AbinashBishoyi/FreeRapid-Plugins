package cz.vity.freerapid.plugins.services.hulkload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class HulkLoadFileRunner extends XFileSharingRunner {

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        return getXFSMethodBuilder(getContentAsString() + "</Form>");     //# missing end form tag
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new HulkLoadFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("hulkload.com/files");
        return downloadPageMarkers;
    }
}