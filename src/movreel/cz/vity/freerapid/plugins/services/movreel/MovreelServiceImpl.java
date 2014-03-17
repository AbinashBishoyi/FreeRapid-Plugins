package cz.vity.freerapid.plugins.services.movreel;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MovreelServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "movreel.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MovreelFileRunner();
    }

}