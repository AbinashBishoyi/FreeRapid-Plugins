package cz.vity.freerapid.plugins.services.fiberupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

public class FiberUploadFileNameHandler implements FileNameHandler {
    @Override
    public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        final Matcher filenameMatcher = PlugUtils.matcher("<h2>Download File :<font.*?> (.+) -", content);
        if (filenameMatcher.find()) {
            final String fileName = filenameMatcher.group(1).trim();
            httpFile.setFileName(fileName);
        } else {
            throw new PluginImplementationException("File name not found");
        }
    }
}
