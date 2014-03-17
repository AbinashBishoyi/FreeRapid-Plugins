package cz.vity.freerapid.plugins.services.henchfile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

public class HenchFileFileNameHandler implements FileNameHandler {
    @Override
    public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "var fname123 = '", "';");
    }
}
