package cz.vity.freerapid.plugins.services.queenshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

public class QueenShareFileSizeHandler implements FileSizeHandler {
    @Override
    public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(PlugUtils.getStringBetween("X" + PlugUtils.getStringBetween(content, "<strong>", "<br>"), "X", "</strong>")));
    }
}
