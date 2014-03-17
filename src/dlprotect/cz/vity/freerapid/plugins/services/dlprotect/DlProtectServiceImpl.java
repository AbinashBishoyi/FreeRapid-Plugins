package cz.vity.freerapid.plugins.services.dlprotect;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class DlProtectServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "dl-protect.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DlProtectFileRunner();
    }

}