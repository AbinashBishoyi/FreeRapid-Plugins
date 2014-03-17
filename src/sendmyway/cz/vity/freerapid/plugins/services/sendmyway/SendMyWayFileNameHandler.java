package cz.vity.freerapid.plugins.services.sendmyway;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

/**
 * @author ntoskrnl
 */
class SendMyWayFileNameHandler implements FileNameHandler {

    @Override
    public void checkFileName(final HttpFile httpFile, final String content) throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("<p\\s*class=\"file-name\"\\s*>\\s*<a[^<>]+?>\\s*(.+?)\\s*</a>\\s*</p>", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(matcher.group(1));
    }

}