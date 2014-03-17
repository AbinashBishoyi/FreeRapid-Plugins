package cz.vity.freerapid.plugins.services.fiberupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FiberUploadFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new FiberUploadFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new FiberUploadFileNameHandler());
        return fileNameHandlers;
    }

    @Override
    public void runCheck() throws Exception {
        if (fileURL.matches("http://(?:www\\.)?bulletupload\\.com/.+")) {
            fileURL = fileURL.replaceFirst("bulletupload\\.com", "fiberupload.com");
        }
        super.runCheck();
    }

    @Override
    protected String getBaseURL() {
        return "http://fiberupload.com/";
    }

    @Override
    protected boolean tryDownloadAndSaveFile(HttpMethod method) throws Exception {
        downloadTask.sleep(6);
        return super.tryDownloadAndSaveFile(method) || super.tryDownloadAndSaveFile(getMethodBuilder().setReferer(fileURL).setAction(method.getURI().toString().replace(httpFile.getFileName(), "GO/" + httpFile.getFileName())).toGetMethod());
    }
}