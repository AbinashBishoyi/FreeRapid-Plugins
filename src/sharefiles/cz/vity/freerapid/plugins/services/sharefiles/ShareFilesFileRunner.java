package cz.vity.freerapid.plugins.services.sharefiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class ShareFilesFileRunner extends XFileSharingRunner {

    @Override
    protected void stepPassword(final MethodBuilder methodBuilder) throws Exception {
        // was finding login's Password
    }

    @Override
    protected void doDownload(final HttpMethod method) throws Exception {
        final String sUri = method.getURI().getURI();
        if (sUri.contains("adf.ly"))
            method.setURI(new URI(sUri.substring(sUri.indexOf("http", 2)), true));
        super.doDownload(method);
    }
}