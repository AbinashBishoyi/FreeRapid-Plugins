package cz.vity.freerapid.plugins.services.tusfiles;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

public class TusFilesFileSizeHandler implements FileSizeHandler {
    @Override
    public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        if (content.contains("<Title>Files of")) {
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(PlugUtils.getStringBetween(content, "<small>(", "total)</small>")));
            return;
        }
        final Matcher match = PlugUtils.matcher(" - ([\\d\\.,]+?\\s*?\\w+?)[\\[<]/", content);
        if (match.find())
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1)));
        else {
            final Matcher match2 = PlugUtils.matcher(">\\s*?Size\\s*?:\\s*?(.+?)\\s*?<", content);
            if (match2.find())
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(match2.group(1)));
            else
                PlugUtils.checkFileSize(httpFile, content, "Size:", "| <");
        }
    }
}
