package cz.vity.freerapid.plugins.services.uplly;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class UpllyFileRunner extends XFileSharingRunner {

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("Wait <span[^>]+?>(\\d+).*?</span");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "<a href\\s?=\\s?(?:\"|')http://adf.ly/.+?/(http.+?" + Pattern.quote(httpFile.getFileName()) + ")(?:\"|')");
        return downloadLinkRegexes;
    }

    @Override
    protected void stepPassword(final MethodBuilder methodBuilder) throws Exception {
        // finding user login, not password protection
        // if Uplly.com actually uses password protection this will need updating
    }
}