package cz.vity.freerapid.plugins.services.xfilesharing;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 * @author ntoskrnl
 */
public abstract class XFileSharingServiceImpl extends AbstractFileShareService {

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

}