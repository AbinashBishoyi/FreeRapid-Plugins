package cz.vity.freerapid.plugins.services.crisshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class CrisShareFileRunner extends XFileSharingRunner {
    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("DOWNLOAD is not available at the moment")) {
            throw new YouHaveToWaitException("DOWNLOAD is not available at the moment, please try again later", 10 * 60);
        }
        super.checkDownloadProblems();
    }
}