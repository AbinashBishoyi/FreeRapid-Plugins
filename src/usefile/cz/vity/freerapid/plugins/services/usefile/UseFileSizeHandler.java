package cz.vity.freerapid.plugins.services.usefile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

public class UseFileSizeHandler implements FileSizeHandler {

    @Override
    public void checkFileSize(final HttpFile httpFile, final String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("File ?[Ss]ize:(?:<[^>]+>)*?([^<]+?)<", content.replaceAll("\\s", ""));
        if (match.find())
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1).trim()));
        else
            PlugUtils.checkFileSize(httpFile, content, "<span class=\"size\">", "</span>");
    }
}
