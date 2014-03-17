package cz.vity.freerapid.plugins.services.xfilesharing.nameandsize;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

/**
 * @author ntoskrnl
 */
public class FileSizeHandlerA implements FileSizeHandler {

    @Override
    public void checkFileSize(final HttpFile httpFile, final String content) throws ErrorDuringDownloadingException {
        Matcher matcher = PlugUtils.matcher("\\(([\\s\\d\\.,]+?(?:bytes|.B|.b))\\s*\\)", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
    }

}
