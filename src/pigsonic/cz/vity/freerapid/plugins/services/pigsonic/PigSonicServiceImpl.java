package cz.vity.freerapid.plugins.services.pigsonic;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class PigSonicServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "pigsonic.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PigSonicFileRunner();
    }

}