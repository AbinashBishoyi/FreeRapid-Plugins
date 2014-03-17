package cz.vity.freerapid.plugins.services.fiberupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

public class FiberUploadFileSizeHandler implements FileSizeHandler {
    @Override
    public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        final Matcher filesizeMatcher = PlugUtils.matcher("(?:.+) - (.+?)</font></h2>", content);
        if (filesizeMatcher.find()) {
            final String fileSize = filesizeMatcher.group(1);
            final long size = PlugUtils.getFileSizeFromString(fileSize);
            httpFile.setFileSize(size);
        } else {
            throw new PluginImplementationException("File size not found");
        }
    }
}
