package cz.vity.freerapid.plugins.services.hitfile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.turbobit.TurboBitFileRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class HitFileFileRunner extends TurboBitFileRunner {

    @Override
    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("Download file.+?<span.+?>(.+?)</span>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(matcher.group(1));
        matcher = getMatcherAgainstContent("File size:</b>\\s*(.+?)</div>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

}