package cz.vity.freerapid.plugins.services.cosmobox;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class CosmoBoxFileRunner extends XFileSharingRunner {

    @Override
    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<h2>Download File", "</h2>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "</font> (", ")</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

}