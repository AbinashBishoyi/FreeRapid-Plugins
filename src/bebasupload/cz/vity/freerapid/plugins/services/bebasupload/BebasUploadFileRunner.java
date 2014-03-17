package cz.vity.freerapid.plugins.services.bebasupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

/**
 * Class which contains main code
 *
 * @author Arthur Gunawan, tong2shot
 */
class BebasUploadFileRunner extends XFileSharingRunner {

    @Override
    protected void stepPassword(MethodBuilder methodBuilder) throws Exception {
        //stepPassword in previous version is broken, not implemented yet, can't find passworded link..
    }
}


