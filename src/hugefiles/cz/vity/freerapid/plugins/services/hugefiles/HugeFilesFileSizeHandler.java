package cz.vity.freerapid.plugins.services.hugefiles;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HugeFilesFileSizeHandler implements FileSizeHandler {

    @Override
    public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher(Pattern.quote(httpFile.getFileName()) + " - (.+?)[\\[<]/", content);
        if (!match.find())
            throw new ErrorDuringDownloadingException("size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1)));
    }
}
