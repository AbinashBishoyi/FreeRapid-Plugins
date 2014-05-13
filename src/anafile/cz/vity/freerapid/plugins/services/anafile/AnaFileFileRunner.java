package cz.vity.freerapid.plugins.services.anafile;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class AnaFileFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "<a[^>]+?href\\s*?=\\s*?['\"](http[^>]+?" + Pattern.quote(httpFile.getFileName()) + ")['\"]");
        return downloadLinkRegexes;
    }

    @Override
    protected boolean handleDirectDownload(final HttpMethod method) throws Exception {
        if (!makeRedirectedRequest(redirectToLocation(method))) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        return false;
    }
}