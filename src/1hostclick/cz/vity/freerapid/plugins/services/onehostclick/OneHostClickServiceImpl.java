package cz.vity.freerapid.plugins.services.onehostclick;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class OneHostClickServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "OneHostClick";
    }

    @Override
    public String getName() {
        return "1hostclick.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new OneHostClickFileRunner();
    }

}