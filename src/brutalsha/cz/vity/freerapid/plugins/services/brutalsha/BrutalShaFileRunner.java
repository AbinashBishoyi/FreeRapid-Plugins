package cz.vity.freerapid.plugins.services.brutalsha;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.FileState;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class BrutalShaFileRunner extends XFileSharingRunner {

    @Override
    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        try {
            if (makeRedirectedRequest(getXFSMethodBuilder().toPostMethod())) {
                checkFileProblems();
                checkFileName();
                checkFileSize();
            }
        } catch (Exception e) { /**/ }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

}