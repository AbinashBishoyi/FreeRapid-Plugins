package cz.vity.freerapid.plugins.services.superupl;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SuperUplServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "SuperUpl";
    }

    @Override
    public String getName() {
        return "superupl.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SuperUplFileRunner();
    }

}