package cz.vity.freerapid.plugins.services.rapidstone;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class RapidStoneServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "RapidStone";
    }

    @Override
    public String getName() {
        return "rapidstone.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RapidStoneFileRunner();
    }

}