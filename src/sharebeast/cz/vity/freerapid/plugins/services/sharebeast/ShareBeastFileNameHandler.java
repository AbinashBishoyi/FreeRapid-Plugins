package cz.vity.freerapid.plugins.services.sharebeast;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

public class ShareBeastFileNameHandler implements FileNameHandler {

    @Override
    public void checkFileName(final HttpFile httpFile, final String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<title>", "</title>");
    }
}
