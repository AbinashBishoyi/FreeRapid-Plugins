package cz.vity.freerapid.plugins.services.boyfriendtv;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class BoyFriendTVServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "boyfriendtv.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BoyFriendTVFileRunner();
    }

}