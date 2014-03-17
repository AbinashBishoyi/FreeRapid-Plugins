package cz.vity.freerapid.plugins.services.qjwm;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

/**
 * @author Tommy[ywx217@gmail.com]
 * Date: 2012-08-07
 * Time: 00:02
 */
public class QjwmFileNameHandlerB implements FileNameHandler{

    @Override
    public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "qjwm.com</a>- ", "</div>");
    }
}
