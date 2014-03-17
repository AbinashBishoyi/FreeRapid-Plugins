package cz.vity.freerapid.plugins.services.ortofiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class OrtoFilesFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new OrtoFilesFileSizeHandler());
        return fileSizeHandlers;
    }
}