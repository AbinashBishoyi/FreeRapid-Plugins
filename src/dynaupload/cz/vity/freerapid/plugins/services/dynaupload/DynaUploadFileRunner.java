package cz.vity.freerapid.plugins.services.dynaupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class DynaUploadFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new DynaUploadFileNameHandler());
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new DynaUploadFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        if (!fileURL.contains("dynaupload")) {
            Matcher match = PlugUtils.matcher("http://.+?(/.+)", fileURL);
            if (!match.find())
                throw new PluginImplementationException("File URL error");
            fileURL = "http://dynaupload.com" + match.group(1);
        }
        super.checkNameAndSize();
    }


}