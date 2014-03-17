package cz.vity.freerapid.plugins.services.ifile_ws;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

public class iFile_wsFileSizeHandler implements FileSizeHandler {
    @Override
    public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkFileSize(httpFile, content, "File Size:", "</");
    }
}
