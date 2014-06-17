package cz.vity.freerapid.plugins.services.gorillavid;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class GorillaVidFileRunner extends XFileSharingRunner {
    private final static Logger logger = Logger.getLogger(GorillaVidFileRunner.class.getName());

    @Override
    protected void correctURL() throws Exception {
        if (fileURL.matches("http://(?:www\\.)?gorillavid\\.com/.+")) {
            fileURL = fileURL.replaceFirst("gorillavid\\.com", "gorillavid.in");
        }
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    @Override
    protected void checkFileName() throws ErrorDuringDownloadingException {
        for (int i = 0; i < 3; i++) {
            try {
                super.checkFileName();
                return;
            } catch (ErrorDuringDownloadingException e) {
                //sometimes they don't provide filename.
                //re-request the page to get filename
                logger.info("Filename not found");
                logger.info("Re-request the page to get filename...");
                HttpMethod method = getGetMethod(fileURL);
                try {
                    makeRedirectedRequest(method);
                } catch (Exception e1) {
                    //
                }
            }
        }
        throw new PluginImplementationException("Filename not found");
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add(0, "file: \"");
        downloadPageMarkers.add(0, "lnk_download\"");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "file: \"(http.+?)\"");
        downloadLinkRegexes.add(0, "lnk_download\" href=\"(http.+?)\""); //skip download manager
        return downloadLinkRegexes;
    }

    @Override
    protected void doDownload(HttpMethod method) throws Exception {
        setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
        super.doDownload(method);
    }
}