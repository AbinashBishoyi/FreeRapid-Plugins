package cz.vity.freerapid.plugins.services.freenapster;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class FreeNapsterServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "free.napster.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FreeNapsterFileRunner();
    }

}