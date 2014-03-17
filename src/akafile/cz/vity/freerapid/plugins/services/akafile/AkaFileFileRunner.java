package cz.vity.freerapid.plugins.services.akafile;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class AkaFileFileRunner extends XFileSharingRunner {
    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    @Override
    protected boolean handleDirectDownload(final HttpMethod method) throws Exception {
        if (!makeRedirectedRequest(redirectToLocation(method))) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        return false;
    }

    @Override
    protected void setLanguageCookie() throws Exception {
        setTextContentTypes("application/cgi");
        super.setLanguageCookie();
    }

}