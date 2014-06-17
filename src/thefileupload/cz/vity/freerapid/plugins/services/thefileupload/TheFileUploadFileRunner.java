package cz.vity.freerapid.plugins.services.thefileupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class TheFileUploadFileRunner extends XFileSharingRunner {
    @Override
    protected void correctURL() throws Exception {
        if (fileURL.contains("efileuploading.com"))
            fileURL = fileURL.replace("efileuploading.com", "thefileupload.com");
        if (fileURL.contains("thefileupload.com"))
            fileURL = fileURL.replace("thefileupload.com", "themediastorage.com");
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new TheFileUploadFileSizeHandler());
        return fileSizeHandlers;
    }
}