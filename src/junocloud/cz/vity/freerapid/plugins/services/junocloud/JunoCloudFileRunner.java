package cz.vity.freerapid.plugins.services.junocloud;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class JunoCloudFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new JunoCloudFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected boolean handleDirectDownload(final HttpMethod method) throws Exception {
        HttpMethod redirMethod = redirectToLocation(method);
        if (!makeRedirectedRequest(redirMethod)) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        fileURL = redirMethod.getURI().toString();
        return false;
    }

}