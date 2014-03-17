package cz.vity.freerapid.plugins.services.prefiles;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author CrazyCoder, Abinash Bishoyi
 */
class PreFilesFileRunner extends XFileSharingRunner {
    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                httpFile.setFileName(PlugUtils.getStringBetween(content, "<div class=\"filename_bar\"><i></i><h3>", " <small>"));
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandler() {
            @Override
            public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(PlugUtils.getStringBetween(content, "<small>(", ")</small></h2></div>")));
            }
        });
        return fileSizeHandlers;
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        return getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("method_free", true)
                .setParameter("method_free", "method_free")
                .setAction(fileURL);
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        super.checkFileProblems();
        if (getContentAsString().contains("The file you were looking for could not be found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }
}
