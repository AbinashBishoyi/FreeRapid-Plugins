package cz.vity.freerapid.plugins.services.hugefiles;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

public class HugeFilesFileNameHandler implements FileNameHandler {

    @Override
    public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("FILENAME:</p>\\s*?<p[^>]*?>(.+?)</", content);
        if (match.find())
            httpFile.setFileName(match.group(1));
        else
            PlugUtils.checkFileSize(httpFile, content, "addthis:title=\"", "\"");
    }
}
