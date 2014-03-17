package cz.vity.freerapid.plugins.services.filebitnet;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author RickCL
 */
class FilebitNetFilesRunner extends XFileSharingRunner {

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "<a style=\"color:#020AF9;\" href=\"(http.+?" + Pattern.quote(httpFile.getFileName()) + ")\"");
        return downloadLinkRegexes;
    }
}
