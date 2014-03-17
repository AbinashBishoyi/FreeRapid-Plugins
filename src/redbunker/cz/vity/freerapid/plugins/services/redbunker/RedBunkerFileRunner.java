package cz.vity.freerapid.plugins.services.redbunker;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class RedBunkerFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new RedBunkerFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("This file is available for Premium Users only")) {
            throw new NotRecoverableDownloadException("This file is only available to premium users");
        }
        super.checkDownloadProblems();
    }
}