package cz.vity.freerapid.plugins.services.igetfile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

public class IGetFileFileNameHandler implements FileNameHandler {

    @Override
    public void checkFileName(final HttpFile httpFile, final String content) throws ErrorDuringDownloadingException {
        Matcher match = PlugUtils.matcher("Downloading:\\s+?(.+?)\\s+?\\(", content);
        if (match.find())
            httpFile.setFileName(match.group(1));
    }
}
