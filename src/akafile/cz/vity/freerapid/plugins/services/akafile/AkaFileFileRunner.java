package cz.vity.freerapid.plugins.services.akafile;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import org.apache.commons.httpclient.Header;
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
    protected boolean handleDirectDownload(HttpMethod method) throws Exception {
        final Header locationHeader = method.getResponseHeader("Location");
        if (locationHeader == null) {
            throw new PluginImplementationException("Invalid redirect");
        }
        method = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(locationHeader.getValue())
                .toGetMethod();
        client.makeRequest(method, true);
        return false;
    }

    @Override
    protected void setLanguageCookie() throws Exception {
        setTextContentTypes("application/cgi");
        super.setLanguageCookie();
    }

}