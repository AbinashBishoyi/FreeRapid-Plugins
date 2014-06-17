package cz.vity.freerapid.plugins.services.filerio;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FileRioFileRunner extends XFileSharingRunner {

    @Override
    protected void correctURL() throws Exception {
        if (fileURL.matches("http://(?:www\\.)?filerio\\.com/.+")) {
            httpFile.setNewURL(new URL(fileURL.replaceFirst("filerio\\.com", "filerio.in")));
        }
    }

    @Override
    protected void checkFileSize() throws ErrorDuringDownloadingException {
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("eval\\(unescape\\('(.+?)'\\)\\)");
        return downloadLinkRegexes;
    }

    @Override
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        for (final String downloadLinkRegex : getDownloadLinkRegexes()) {
            final Matcher matcher = getMatcherAgainstContent(downloadLinkRegex);
            if (matcher.find()) {
                try {
                    final String unEscStr = URLDecoder.decode(matcher.group(1), "UTF-8");
                    return PlugUtils.getStringBetween(unEscStr, "location.href=\"", "\";");
                } catch (UnsupportedEncodingException e) {
                    throw new PluginImplementationException("Error reading download URL");
                }
            }
        }
        throw new PluginImplementationException("Download link not found");
    }

    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        super.checkDownloadProblems();
    }

}