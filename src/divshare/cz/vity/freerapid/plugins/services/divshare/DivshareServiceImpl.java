package cz.vity.freerapid.plugins.services.divshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity, ntoskrnl
 */
public class DivshareServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "divshare.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DivshareFileRunner();
    }

}
