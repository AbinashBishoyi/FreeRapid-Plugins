package cz.vity.freerapid.plugins.services.ifile_ws;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class iFile_wsFileRunner extends XFileSharingRunner {
    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new iFile_wsFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected void stepPassword(final MethodBuilder methodBuilder) throws Exception {
        // catching on username/Password - Files not secured with a password
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "window.location\\s?=\\s?(?:\"|')(http.+?" + Pattern.quote(httpFile.getFileName()) + ")(?:\"|')");
        return downloadLinkRegexes;
    }
}