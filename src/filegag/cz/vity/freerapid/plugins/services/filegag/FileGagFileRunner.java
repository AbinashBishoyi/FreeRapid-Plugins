package cz.vity.freerapid.plugins.services.filegag;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FileGagFileRunner extends XFileSharingRunner {
    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "product_download_url=(http.+?" + Pattern.quote(httpFile.getFileName()) + ")&");
        return downloadLinkRegexes;
    }
}