package cz.vity.freerapid.plugins.services.serienjunkies;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author benpicco
 */
public class SerienjunkiesServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "serienjunkies.org";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SerienjunkiesFileRunner();
    }

}
