package cz.vity.freerapid.plugins.services.clicktoview;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerRunner;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class ClickToViewFileRunner extends XFilePlayerRunner {

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "['\"]?file['\"]?\\s*?,\\s*?['\"](http[^'\"]+?)['\"]");
        return downloadLinkRegexes;
    }

}