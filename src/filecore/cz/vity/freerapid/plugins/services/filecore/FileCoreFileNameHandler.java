package cz.vity.freerapid.plugins.services.filecore;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

public class FileCoreFileNameHandler implements FileNameHandler {
    @Override
    public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        httpFile.setFileName(PlugUtils.getStringBetween(content, "\"Download File ", "\"").trim().replaceAll("\\s", "."));
    }
}
