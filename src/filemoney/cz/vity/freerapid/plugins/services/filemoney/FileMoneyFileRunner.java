package cz.vity.freerapid.plugins.services.filemoney;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FileMoneyFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.remove(0);  // FileNameHandlerA
        return fileNameHandlers;
    }

}