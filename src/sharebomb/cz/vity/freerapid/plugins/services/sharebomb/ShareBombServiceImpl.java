package cz.vity.freerapid.plugins.services.sharebomb;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Eterad
 */
public class ShareBombServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "sharebomb.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShareBombFileRunner();
    }

}