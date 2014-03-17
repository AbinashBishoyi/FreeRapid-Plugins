package cz.vity.freerapid.plugins.services.fireget;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

/**
 * @author birchie
 */
public class FireGetFileSizeHandler implements FileSizeHandler {

    @Override
    public void checkFileSize(final HttpFile httpFile, final String content) throws ErrorDuringDownloadingException {
        Matcher matcher = PlugUtils.matcher("ize\\s*?:\\s*?<(?:[^>]+?><)*[^>]+?>(.+?)<", content.replaceAll(">\\s+?<", "><"));
        if (!matcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
    }

}
