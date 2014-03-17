package cz.vity.freerapid.plugins.services.medafire;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class MedaFireFileRunner extends XFileSharingRunner {
    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        return getXFSMethodBuilder(getContentAsString() + "</Form>");     //# missing end form tag
    }
}