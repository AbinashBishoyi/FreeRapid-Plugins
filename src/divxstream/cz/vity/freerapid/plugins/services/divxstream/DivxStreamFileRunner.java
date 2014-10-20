package cz.vity.freerapid.plugins.services.divxstream;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerRunner;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class DivxStreamFileRunner extends XFilePlayerRunner {

    protected void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("404 Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        super.checkFileProblems();
    }

}