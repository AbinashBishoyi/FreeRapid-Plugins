package cz.vity.freerapid.plugins.services.rapidstone;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 * @author ntoskrnl
 */
class RapidStoneFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new FileSizeHandler() {
            @Override
            public void checkFileSize(final HttpFile httpFile, final String content) throws ErrorDuringDownloadingException {
                PlugUtils.checkFileSize(httpFile, content, "[", "]<");
            }
        });
        return fileSizeHandlers;
    }

    @Override
    protected void checkFileName() throws ErrorDuringDownloadingException {
        super.checkFileName();
        //Remove "[filesize]" at the end
        final Matcher matcher = PlugUtils.matcher("^(.+?) \\[[^\\[\\]]+?\\]$", httpFile.getFileName());
        if (matcher.find()) {
            httpFile.setFileName(matcher.group(1));
        }
    }

}