package cz.vity.freerapid.plugins.services.rocketfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class RocketFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "RocketFile";
    }

    @Override
    public String getName() {
        return "rocketfile.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RocketFileFileRunner();
    }

}