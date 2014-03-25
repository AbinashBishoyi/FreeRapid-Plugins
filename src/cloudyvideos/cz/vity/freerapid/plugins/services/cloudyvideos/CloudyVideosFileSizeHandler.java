package cz.vity.freerapid.plugins.services.cloudyvideos;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

public class CloudyVideosFileSizeHandler implements FileSizeHandler {
    @Override
    public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher(" - ([\\d\\.,]+?\\s*?\\w+?)[\\[<]/", content);
        if (match.find())
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1)));
        else
            PlugUtils.checkFileSize(httpFile, content, ";&nbsp;", "</h3>");
    }
}
