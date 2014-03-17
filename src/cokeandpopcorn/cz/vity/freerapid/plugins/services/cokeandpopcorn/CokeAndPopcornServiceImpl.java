package cz.vity.freerapid.plugins.services.cokeandpopcorn;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class CokeAndPopcornServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "cokeandpopcorn.ch";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CokeAndPopcornFileRunner();
    }

}