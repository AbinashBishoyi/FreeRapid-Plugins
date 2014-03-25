package cz.vity.freerapid.plugins.services.cloudyvideos;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

public class CloudyVideosFileNameHandler implements FileNameHandler {
    @Override
    public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("[\\]>]([^\\]>]+?) - [\\d\\.,]+?\\s*?\\w+?[\\[<]/", content);
        if (match.find())
            httpFile.setFileName(match.group(1).trim());
        else
            PlugUtils.checkName(httpFile, content, "<h3>Watch &nbsp;", "&nbsp;");
    }
}
