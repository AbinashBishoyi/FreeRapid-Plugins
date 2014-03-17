package cz.vity.freerapid.plugins.services.hulkload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class HulkLoadFileRunner extends XFileSharingRunner {

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        return getXFSMethodBuilder(getContentAsString() + "</Form>");     //# missing end form tag
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new HulkLoadFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("hulkload.com/files");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "<a.+?href\\s?=\\s?(?:\"|')(http.+?" + Pattern.quote(httpFile.getFileName()) + ")(?:\"|')");   // only change  <a.+?href
        return downloadLinkRegexes;
    }

    @Override
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        String url = super.getDownloadLinkFromRegexes();
        if (!url.contains("adf.ly"))
            return url;
        url = url.replaceFirst("http", "");
        return url.substring(url.indexOf("http"));
    }
}