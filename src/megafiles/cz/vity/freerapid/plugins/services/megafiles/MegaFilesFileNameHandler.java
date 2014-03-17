package cz.vity.freerapid.plugins.services.megafiles;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

public class MegaFilesFileNameHandler implements FileNameHandler {
    @Override
    public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("<div align=\"center\".*?>\\s+?<b>(.+?)</b>", content);
        if (!match.find())
            throw new ErrorDuringDownloadingException("File name not found");
        httpFile.setFileName(match.group(1).trim());
    }
}
