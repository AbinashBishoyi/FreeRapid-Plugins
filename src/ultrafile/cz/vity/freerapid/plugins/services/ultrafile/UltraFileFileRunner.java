package cz.vity.freerapid.plugins.services.ultrafile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class UltraFileFileRunner extends XFileSharingRunner {

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("var download_url\\s*?=\\s*?[\"|'](http.+?" + Pattern.quote(httpFile.getFileName()) + ")[\"|']");
        return downloadLinkRegexes;
    }
}