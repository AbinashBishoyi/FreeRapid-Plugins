package cz.vity.freerapid.plugins.services.filesabc;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FilesABCFileRunner extends XFileSharingRunner {

    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("You can download files up to")) {
            throw new NotRecoverableDownloadException(PlugUtils.getStringBetween(content, "class=\"err\">", "<br>"));
        }
        super.checkDownloadProblems();
    }
}