package cz.vity.freerapid.plugins.services.berofile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class BeroFileFileRunner extends XFileSharingRunner {
    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("Click Here To Download");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "<a.+?href\\s?=\\s?(?:\"|')(http.+?" + Pattern.quote(httpFile.getFileName()) + ")(?:\"|')");   // only change  <a.+?href
        return downloadLinkRegexes;
    }
}