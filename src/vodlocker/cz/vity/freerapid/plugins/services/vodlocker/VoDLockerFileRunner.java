package cz.vity.freerapid.plugins.services.vodlocker;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerRunner;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class VoDLockerFileRunner extends XFilePlayerRunner {

    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Skipped countdown")) {
        }
        super.checkDownloadProblems();
    }

    @Override
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        final String link = super.getDownloadLinkFromRegexes();
        final String ext = link.substring(link.lastIndexOf("."));
        if (!httpFile.getFileName().matches(".+?" + ext))
            httpFile.setFileName(httpFile.getFileName() + ext);
        return link;
    }
}