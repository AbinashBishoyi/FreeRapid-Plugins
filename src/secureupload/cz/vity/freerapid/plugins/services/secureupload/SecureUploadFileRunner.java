package cz.vity.freerapid.plugins.services.secureupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class SecureUploadFileRunner extends XFileSharingRunner {

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("You will download this file in free mode");
        return downloadPageMarkers;
    }

    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "href\\s?=\\s?(?:\"|')(http[^\"']+?secureupload[^\"']+?" + Pattern.quote(httpFile.getFileName()) + ")(?:\"|')");
        return downloadLinkRegexes;
    }

    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Only Premium members can access this file")) {
            throw new NotRecoverableDownloadException("Only Premium members can access this file");
        }
        super.checkDownloadProblems();
    }

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("id=\"showsec\">(\\d+)</span>");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1).trim()) + 1;
        }
        return 0;
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("file you were looking for could not be found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        super.checkFileProblems();
    }

}