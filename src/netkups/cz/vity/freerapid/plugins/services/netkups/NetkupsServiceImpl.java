package cz.vity.freerapid.plugins.services.netkups;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class NetkupsServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "netkups.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new NetkupsFileRunner();
    }

}