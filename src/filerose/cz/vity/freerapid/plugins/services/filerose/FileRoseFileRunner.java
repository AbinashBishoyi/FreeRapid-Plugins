package cz.vity.freerapid.plugins.services.filerose;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FileRoseFileRunner extends XFileSharingRunner {
    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(fileNameHandlers.remove(0)); //put FileHandlerA to the end of list
        return fileNameHandlers;
    }
}