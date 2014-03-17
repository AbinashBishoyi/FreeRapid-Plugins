package cz.vity.freerapid.plugins.services.shantibit;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ShantiBitServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ShantiBit";
    }

    @Override
    public String getName() {
        return "shantibit.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShantiBitFileRunner();
    }

}