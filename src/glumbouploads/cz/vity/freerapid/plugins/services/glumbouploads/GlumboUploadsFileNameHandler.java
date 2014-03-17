package cz.vity.freerapid.plugins.services.glumbouploads;

import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;

import java.util.regex.Matcher;

public class GlumboUploadsFileNameHandler implements FileNameHandler {
    @Override
    public void checkFileName(final HttpFile httpFile, final String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "attr('value', '", "');");
    }
}
