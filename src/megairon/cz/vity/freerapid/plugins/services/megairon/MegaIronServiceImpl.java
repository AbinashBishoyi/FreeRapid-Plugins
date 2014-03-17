package cz.vity.freerapid.plugins.services.megairon;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MegaIronServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MegaIron";
    }

    @Override
    public String getName() {
        return "megairon.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MegaIronFileRunner();
    }

}