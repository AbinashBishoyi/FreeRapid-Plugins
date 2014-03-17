package cz.vity.freerapid.plugins.services.filerio;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FileRioFileRunner extends XFileSharingRunner {

    @Override
    public void runCheck() throws Exception {
        if (fileURL.matches("http://(?:www\\.)?filerio\\.com/.+")) {
            httpFile.setNewURL(new URL(fileURL.replaceFirst("filerio\\.com", "filerio.in")));
        }
        super.runCheck();
    }

    @Override
    protected void checkFileSize() throws ErrorDuringDownloadingException {
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("eval\\(unescape\\('%(.+?)'\\)\\)");
        return downloadLinkRegexes;
    }

    private final static Logger logger = Logger.getLogger(FileRioFileRunner.class.getName());

    @Override
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        for (final String downloadLinkRegex : getDownloadLinkRegexes()) {
            final Matcher matcher = getMatcherAgainstContent(downloadLinkRegex);
            if (matcher.find()) {
                final String[] escArr = matcher.group(1).split("%");
                String unEscStr = "";
                for (String chr : escArr) {
                    unEscStr += new String(Character.toChars(Integer.parseInt(chr, 16)));
                }
                return PlugUtils.getStringBetween(unEscStr, "location.href=\"", "\";");
            }
        }
        throw new PluginImplementationException("Download link not found");
    }
}