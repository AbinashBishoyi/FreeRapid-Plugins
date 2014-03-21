package cz.vity.freerapid.plugins.services.tusfiles;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

public class TusFilesFileNameHandler implements FileNameHandler {

    @Override
    public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        if (content.contains("<b>Files of</b>")) {
            httpFile.setFileName("Folder > " + PlugUtils.getStringBetween(content, "Files of</b> <small>", "</small>"));
            return;
        }
        final Matcher match = PlugUtils.matcher("</a>\\s+?</li>\\s+?<li>(.+?)</li>\\s+?<li><b>Size", content);
        if (match.find())
            httpFile.setFileName(match.group(1).trim());
        else {
            try {
                PlugUtils.checkName(httpFile, content, "globalFileName = '", "';");
            } catch (Exception e) {
                PlugUtils.checkName(httpFile, content, "/?q=", "\" ");
            }
        }
    }
}
