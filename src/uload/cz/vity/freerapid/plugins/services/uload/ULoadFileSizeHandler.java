package cz.vity.freerapid.plugins.services.uload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

public class ULoadFileSizeHandler implements FileSizeHandler {
    @Override
    public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("<h2>.*-\\s*(.*)<", content);
        if (matcher.find())
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
    }
}
