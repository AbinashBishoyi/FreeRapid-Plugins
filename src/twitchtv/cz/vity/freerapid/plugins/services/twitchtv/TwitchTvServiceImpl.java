package cz.vity.freerapid.plugins.services.twitchtv;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class TwitchTvServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "twitch.tv";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TwitchTvFileRunner();
    }

}