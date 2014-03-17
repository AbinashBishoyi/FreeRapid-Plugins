package cz.vity.freerapid.plugins.services.fiberstorage;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FiberStorageFileRunner extends XFileSharingRunner {

    @Override
    protected void stepPassword(final MethodBuilder methodBuilder) throws Exception {
        // incorrectly catching username/Password
    }
}