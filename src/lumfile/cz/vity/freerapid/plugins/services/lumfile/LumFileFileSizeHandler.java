package cz.vity.freerapid.plugins.services.lumfile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

public class LumFileFileSizeHandler implements FileSizeHandler {
    @Override
    public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        final Matcher filenameMatcher = PlugUtils.matcher("File:.*\\[(.+)\\]</h2>", content);
        if (filenameMatcher.find()) {
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(filenameMatcher.group(1)));
        }
    }
}
