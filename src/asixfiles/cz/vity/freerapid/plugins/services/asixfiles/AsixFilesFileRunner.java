package cz.vity.freerapid.plugins.services.asixfiles;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class AsixFilesFileRunner extends XFileSharingRunner {

    @Override
    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String nameAndSizeRule = "You have requested.*?http://(?:www\\.)?" + "asixfiles.com" + "/[a-z0-9]{12}/(.*)</font> \\((.*?)\\)</font>$";
        final Matcher matcher = getMatcherAgainstContent(nameAndSizeRule);
        if (!matcher.find()) {
            throw new PluginImplementationException("File name and size not found");
        }
        httpFile.setFileName(matcher.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2).trim()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

}