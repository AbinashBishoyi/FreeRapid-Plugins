package cz.vity.freerapid.plugins.services.onehostclick;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class OneHostClickServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "1hostclick.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new OneHostClickFileRunner();
    }

}