package cz.vity.freerapid.plugins.services.fiberupload;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;
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
            httpFile.setNewURL(new URL(fileURL.replaceFirst("bulletupload\\.com", "fiberupload.com")));
        }
        super.runCheck();
    }

    @Override
    protected HttpMethod stepRedirectToFileLocation(HttpMethod method) throws Exception {
        final Header locationHeader = method.getResponseHeader("Location");
        if (locationHeader == null) {
            throw new PluginImplementationException("Invalid redirect");
        }
        final String downloadFileURL = locationHeader.getValue();
        downloadTask.sleep(6);
        return getMethodBuilder()
                .setReferer(downloadFileURL)
                .setAction(downloadFileURL.replaceAll(httpFile.getFileName(), "GO/" + httpFile.getFileName()))
                .toGetMethod();
    }
}