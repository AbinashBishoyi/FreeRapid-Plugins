package cz.vity.freerapid.plugins.services.uploadhunt;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class UploadHuntFileRunner extends XFileSharingRunner {

    @Override
    protected void stepPassword(final MethodBuilder methodBuilder) throws Exception {
        // password protection incorrectly detected premium login input
    }
}