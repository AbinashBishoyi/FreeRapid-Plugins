package cz.vity.freerapid.plugins.services.xfilesharing.nameandsize;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

/**
 * @author birchie
 */
public class FileSizeHandlerB implements FileSizeHandler {

    @Override
    public void checkFileSize(final HttpFile httpFile, final String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkFileSize(httpFile, content, "<span>File size:", "</span>");
    }

}
