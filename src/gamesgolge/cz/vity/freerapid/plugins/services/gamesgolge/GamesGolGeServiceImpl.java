package cz.vity.freerapid.plugins.services.gamesgolge;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class GamesGolGeServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "gamesgolge.ge";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GamesGolGeFileRunner();
    }

}