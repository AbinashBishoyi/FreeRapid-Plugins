package cz.vity.freerapid.plugins.services.sharesix;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u2
 */
class ShareSixFileRunner extends XFileSharingRunner {
    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                final Matcher matcher = Pattern.compile("Filename:.*?<p>(.+?)</p", Pattern.DOTALL).matcher(content);
                if (!matcher.find()) {
                    throw new PluginImplementationException("File name not found");
                }
                httpFile.setFileName(matcher.group(1).trim());
            }
        });
        return fileNameHandlers;
    }
}