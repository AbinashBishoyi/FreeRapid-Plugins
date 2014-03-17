package cz.vity.freerapid.plugins.services.clipshouse;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ClipsHouseServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "clipshouse.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ClipsHouseFileRunner();
    }

}