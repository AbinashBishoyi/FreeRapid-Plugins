package cz.vity.freerapid.plugins.services.go4up;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Go4upServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "go4up.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Go4upFileRunner();
    }

}