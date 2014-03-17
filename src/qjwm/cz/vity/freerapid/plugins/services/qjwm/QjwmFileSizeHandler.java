package cz.vity.freerapid.plugins.services.qjwm;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

/**
 * @author Tommy[ywx217@gmail.com]
 * Date: 2012-08-07
 * Time: 00:06
 */
public class QjwmFileSizeHandler implements FileSizeHandler{
    @Override
    public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        Matcher sizeMatcher = PlugUtils.matcher(":[^\\d]+([0-9\\. KMGT]+)", content);
        if (!sizeMatcher.find())
            throw new ErrorDuringDownloadingException("Cannot read file size from page.");
        final String strFileSize = sizeMatcher.group(1) + "B";

        PlugUtils.getFileSizeFromString(strFileSize);
    }
}
