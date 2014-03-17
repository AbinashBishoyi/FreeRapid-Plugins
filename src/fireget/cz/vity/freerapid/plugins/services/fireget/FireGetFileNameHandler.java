package cz.vity.freerapid.plugins.services.fireget;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

/**
 * @author birchie
 */
public class FireGetFileNameHandler implements FileNameHandler {

    @Override
    public void checkFileName(final HttpFile httpFile, final String content) throws ErrorDuringDownloadingException {

        final Matcher matcher = PlugUtils.matcher("File(?:name)?\\s*?:\\s*?<(?:[^>]+?><)*[^>]+?>(.+?)<", content.replaceAll(">\\s+?<", "><"));
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(PlugUtils.unescapeHtml(matcher.group(1)).trim());
    }

}
