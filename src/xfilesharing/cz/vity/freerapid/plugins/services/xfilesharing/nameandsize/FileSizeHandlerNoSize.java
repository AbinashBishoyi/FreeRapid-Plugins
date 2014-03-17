package cz.vity.freerapid.plugins.services.xfilesharing.nameandsize;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;

public class FileSizeHandlerNoSize implements FileSizeHandler {
    @Override
    public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        // No file size displayed
    }
}
