package cz.vity.freerapid.plugins.services.megashares;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class MegaSharesServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "megashares.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MegaSharesFileRunner();
    }

}