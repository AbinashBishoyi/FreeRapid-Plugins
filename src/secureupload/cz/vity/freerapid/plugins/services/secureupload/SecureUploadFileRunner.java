package cz.vity.freerapid.plugins.services.secureupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class SecureUploadFileRunner extends XFileSharingRunner {

    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("href=\"([^\"]*)\"><span>Download");
        return downloadLinkRegexes;
    }

    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Only Premium members can access this file")) {
            throw new ServiceConnectionProblemException("Only Premium members can access this file");
        }
        super.checkDownloadProblems();
    }
}