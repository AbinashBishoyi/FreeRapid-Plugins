package cz.vity.freerapid.plugins.services.musickapoz;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

public class MusicKapozFileNameHandler implements FileNameHandler {
    @Override
    public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("Filename:</b></td><td.*?>(.+?)</td>", content);
        if (!match.find()) throw new ErrorDuringDownloadingException();
        httpFile.setFileName(match.group(1).trim());
    }
}
