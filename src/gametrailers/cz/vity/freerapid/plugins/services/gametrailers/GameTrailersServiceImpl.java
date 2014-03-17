package cz.vity.freerapid.plugins.services.gametrailers;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class GameTrailersServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "gametrailers.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GameTrailersFileRunner();
    }

}
