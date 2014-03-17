package cz.vity.freerapid.plugins.services.vidto;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class VidToServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "VidTo";
    }

    @Override
    public String getName() {
        return "vidto.me";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VidToFileRunner();
    }

}