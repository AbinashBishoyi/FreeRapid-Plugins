package cz.vity.freerapid.plugins.services.hugefiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 * @author ntoskrnl
 */
class HugeFilesFileRunner extends XFileSharingRunner {

    @Override
    protected MethodBuilder getXFSMethodBuilder(final String content) throws Exception {
        //the mobile version has a different download form, grab the full version
        final int index = content.indexOf("<div class=\"full-version\">");
        return super.getXFSMethodBuilder(content.substring(index + 1));
    }

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new HugeFilesFileNameHandler());
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new HugeFilesFileSizeHandler());
        return fileSizeHandlers;
    }

}