package cz.vity.freerapid.plugins.services.shareprofi;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class ShareProfiFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.remove(0);
        return fileNameHandlers;
    }

    @Override
    protected HttpMethod redirectToLocation(final HttpMethod method) throws Exception {
        final Header locationHeader = method.getResponseHeader("Location");
        if (locationHeader == null) {
            throw new PluginImplementationException("Invalid redirect");
        }
        String action = locationHeader.getValue();
        if (action.contains(":////"))
            action = action.replaceFirst(":////", "://");
        return getMethodBuilder()
                .setReferer(fileURL)
                .setAction(action)
                .toGetMethod();
    }

}