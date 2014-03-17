package cz.vity.freerapid.plugins.services.rockdizfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class RockDizFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "RockDizFile";
    }

    @Override
    public String getName() {
        return "rockdizfile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RockDizFileFileRunner();
    }
}