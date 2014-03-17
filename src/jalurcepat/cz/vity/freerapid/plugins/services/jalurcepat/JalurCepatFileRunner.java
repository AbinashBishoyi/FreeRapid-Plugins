package cz.vity.freerapid.plugins.services.jalurcepat;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class JalurCepatFileRunner extends XFileSharingRunner {

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found") || contentAsString.contains("No such file") || contentAsString.contains("<font class=\"err\">")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        super.checkFileProblems();
    }

}