package cz.vity.freerapid.plugins.services.mixshared;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import org.apache.commons.httpclient.HttpMethod;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class MixSharedFileRunner extends XFileSharingRunner {
    @Override
    protected boolean tryDownloadAndSaveFile(HttpMethod method) throws Exception {
        setClientParameter(DownloadClientConsts.NO_CONTENT_LENGTH_AVAILABLE, true); //they use "Content-length" as field name, not "Content-Length" which is the standard one
        return super.tryDownloadAndSaveFile(method);
    }

}