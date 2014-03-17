package cz.vity.freerapid.plugins.services.ryushare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class RyuShareFileRunner extends XFileSharingRunner {

    @Override
    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "Download File", "</h2>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "</font> (", ")</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

}