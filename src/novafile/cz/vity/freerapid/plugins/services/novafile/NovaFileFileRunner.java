package cz.vity.freerapid.plugins.services.novafile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class NovaFileFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new NovaFileFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        final int index = httpFile.getFileName().indexOf("&#8230;");
        if (index > 0) {
            final String s = httpFile.getFileName().substring(0, index);
            downloadLinkRegexes.add("<a href\\s?=\\s?(?:\"|')(http.+?" + Pattern.quote(s) + ".+?)(?:\"|')");
        }
        return downloadLinkRegexes;
    }

}