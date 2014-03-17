package cz.vity.freerapid.plugins.services.uploadic;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class UploadicFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandler() {
            @Override
            public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                //Page does not always show file size (else caught by FileSizeHandlerA)
            }
        });
        return fileSizeHandlers;
    }

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("Wait\\s*<span[^>]+?>\\s*(\\d+)\\s*</span");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }


}