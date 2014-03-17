package cz.vity.freerapid.plugins.services.spicyfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SpicyFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "SpicyFile";
    }

    @Override
    public String getName() {
        return "spicyfile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SpicyFileFileRunner();
    }

}