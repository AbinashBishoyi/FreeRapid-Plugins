package cz.vity.freerapid.plugins.services.filestay;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie\
 */
class FileStayFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new FileStayFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("href\\s?=\\s?(?:\"|')(http.+?" + Pattern.quote(httpFile.getFileName()) + ")(?:\"|')");
        return downloadLinkRegexes;
    }
}