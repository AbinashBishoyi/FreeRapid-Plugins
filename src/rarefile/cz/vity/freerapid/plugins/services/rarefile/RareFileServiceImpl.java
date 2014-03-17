package cz.vity.freerapid.plugins.services.rarefile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class RareFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "RareFile";
    }

    @Override
    public String getName() {
        return "rarefile.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RareFileFileRunner();
    }
}