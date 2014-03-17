package cz.vity.freerapid.plugins.services.sendmyway;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class SendMyWayFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new SendMyWayFileNameHandler());
        return fileNameHandlers;
    }

}