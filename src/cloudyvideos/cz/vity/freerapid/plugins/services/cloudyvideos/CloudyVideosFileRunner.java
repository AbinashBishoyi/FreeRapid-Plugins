package cz.vity.freerapid.plugins.services.cloudyvideos;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class CloudyVideosFileRunner extends XFilePlayerRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new CloudyVideosFileNameHandler());
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new CloudyVideosFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder(final String content) throws Exception {
        return getXFSMethodBuilder(content, "method_");
    }
}