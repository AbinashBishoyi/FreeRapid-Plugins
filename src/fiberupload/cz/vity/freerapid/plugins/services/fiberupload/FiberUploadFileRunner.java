package cz.vity.freerapid.plugins.services.fiberupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FiberUploadFileRunner extends XFileSharingRunner {
    private final static Logger logger = Logger.getLogger(FiberUploadFileRunner.class.getName());

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
    protected void correctURL() throws Exception {
        if (fileURL.matches("http://(?:www\\.)?bulletupload\\.com/.+")) {
            httpFile.setNewURL(new URL(fileURL.replaceFirst("bulletupload\\.com", "fiberupload.com")));
        }
    }

    @Override
    protected String getBaseURL() {
        return "http://fiberupload.com/";
    }

    @Override
    protected boolean tryDownloadAndSaveFile(HttpMethod method) throws Exception {
        for (int i = 0; i < 3; i++) { //retry to save file if the file not ready/being prepared. 
            downloadTask.sleep(6);
            if (super.tryDownloadAndSaveFile(getMethodBuilder().setReferer(fileURL).setAction(method.getURI().toString()).toGetMethod())) //"cloning" method, to prevent method being aborted.
                return true;
            logger.warning(getContentAsString());
            downloadTask.sleep(6);
            if (super.tryDownloadAndSaveFile(getMethodBuilder().setReferer(fileURL).setAction(method.getURI().toString().replace(httpFile.getFileName(), "GO/" + httpFile.getFileName())).toGetMethod()))
                return true;
            logger.warning(getContentAsString());
        }
        return false;
    }
}