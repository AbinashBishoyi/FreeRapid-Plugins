package cz.vity.freerapid.plugins.services.uploadc;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class UploadCFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new FileSizeHandler() {
            @Override
            public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                Matcher match = PlugUtils.matcher("File Size :</strong> </span><span class=\"file_code\">\\s*(.*)\\s*</span>", content);
                if (!match.find())
                    throw new ErrorDuringDownloadingException("File size not found");
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1)));
            }
        });
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = new LinkedList<String>();
        downloadLinkRegexes.add("'file','(http.+?" + Pattern.quote(httpFile.getFileName()) + ")'");
        return downloadLinkRegexes;
    }

}