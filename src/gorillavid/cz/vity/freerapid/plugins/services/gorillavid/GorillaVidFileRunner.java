package cz.vity.freerapid.plugins.services.gorillavid;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class GorillaVidFileRunner extends XFileSharingRunner {

    @Override
    protected void setLanguageCookie() throws Exception {
        if (fileURL.matches("http://(?:www\\.)?gorillavid\\.com/.+")) {
            fileURL = fileURL.replaceFirst("gorillavid\\.com", "gorillavid.in");
        }
        super.setLanguageCookie();
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add(0, "file: \"");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "file: \"(http.+?)\"");
        return downloadLinkRegexes;
    }

    @Override
    protected void doDownload(HttpMethod method) throws Exception {
        setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
        super.doDownload(method);
    }
}