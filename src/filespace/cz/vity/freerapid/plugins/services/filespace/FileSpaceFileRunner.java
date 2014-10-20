package cz.vity.freerapid.plugins.services.filespace;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FileSpaceFileRunner extends XFileSharingRunner {

    @Override
    protected void checkFileSize() throws ErrorDuringDownloadingException {
        for (final FileSizeHandler fileSizeHandler : getFileSizeHandlers()) {
            try {
                fileSizeHandler.checkFileSize(httpFile, getContentAsString().replace("&nbsp;", " "));
                //logger.info("Size handler: " + fileSizeHandler.getClass().getSimpleName());
                return;
            } catch (final ErrorDuringDownloadingException e) {
                //failed
            }
        }
        throw new PluginImplementationException("File size not found");
    }

    @Override
    protected void correctURL() throws Exception {
        if (fileURL.contains("spaceforfiles.com/"))
            fileURL = fileURL.replace("spaceforfiles.com/", "filespace.com/");
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder(final String content) throws Exception {
        return getXFSMethodBuilder(content + "</Form>", "method_free");
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("No such file with this filename")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        super.checkFileProblems();
    }

    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("file is available for Premium Users only")) {
            throw new NotRecoverableDownloadException("This file is only available to premium users");
        }
        super.checkDownloadProblems();
    }
}