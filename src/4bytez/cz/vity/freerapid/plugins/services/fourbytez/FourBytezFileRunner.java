package cz.vity.freerapid.plugins.services.fourbytez;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FourBytezFileRunner extends XFileSharingRunner {
    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Web Server may be down")) {
            throw new ServiceConnectionProblemException("Web Server may be down, too busy, or experiencing other problems");
        }
        super.checkDownloadProblems();
    }
}