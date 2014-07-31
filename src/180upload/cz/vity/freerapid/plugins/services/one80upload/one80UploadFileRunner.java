package cz.vity.freerapid.plugins.services.one80upload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class one80UploadFileRunner extends XFileSharingRunner {

    @Override
    protected void correctURL() throws Exception {
        if (fileURL.contains("180upload.nl/"))
            fileURL = fileURL.replaceFirst("180upload.nl/", "180upload.com/");
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("By downloading the file you agree to the TOS");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "var file_link\\s*?=\\s*?['\"](.+?)['\"]");
        return downloadLinkRegexes;
    }
}