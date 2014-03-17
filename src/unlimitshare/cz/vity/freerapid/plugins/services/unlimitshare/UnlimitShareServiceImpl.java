package cz.vity.freerapid.plugins.services.unlimitshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UnlimitShareServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "unlimitshare.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UnlimitShareFileRunner();
    }

}