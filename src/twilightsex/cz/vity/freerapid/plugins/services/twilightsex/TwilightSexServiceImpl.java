package cz.vity.freerapid.plugins.services.twilightsex;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class TwilightSexServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "twilightsex.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TwilightSexFileRunner();
    }

}