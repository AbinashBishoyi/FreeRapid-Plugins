package cz.vity.freerapid.plugins.services.hundredshared;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class HundredSharedServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "100Shared";
    }

    @Override
    public String getName() {
        return "100shared.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HundredSharedFileRunner();
    }

}