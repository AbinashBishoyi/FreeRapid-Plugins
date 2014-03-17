package cz.vity.freerapid.plugins.services.filerio;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.net.URL;

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
}