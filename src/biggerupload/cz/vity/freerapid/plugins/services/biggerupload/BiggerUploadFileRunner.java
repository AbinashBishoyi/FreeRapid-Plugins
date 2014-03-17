package cz.vity.freerapid.plugins.services.biggerupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class BiggerUploadFileRunner extends XFileSharingRunner {
    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        return super.getXFSMethodBuilder().setParameter("method_premium", "");
    }
}