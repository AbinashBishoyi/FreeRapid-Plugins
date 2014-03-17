package cz.vity.freerapid.plugins.services.fileking;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FileKingFileRunner extends XFileSharingRunner {

    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("http://fileking.co/player/player.swf");
        return downloadPageMarkers;
    }

    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("file=(.+?)&");
        return downloadLinkRegexes;
    }

}