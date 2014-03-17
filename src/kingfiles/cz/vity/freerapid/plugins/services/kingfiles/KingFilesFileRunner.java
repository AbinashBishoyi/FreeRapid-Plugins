package cz.vity.freerapid.plugins.services.kingfiles;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class KingFilesFileRunner extends XFileSharingRunner {

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File Not Found")) {
            if (!content.contains("visibility:hidden\"><b>File Not Found"))
                throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("file was removed")
                || content.contains("file has been removed")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("server is in maintenance mode")) {
            throw new ServiceConnectionProblemException("This server is in maintenance mode. Please try again later.");
        }
    }
}