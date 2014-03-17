package cz.vity.freerapid.plugins.services.vidbux;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class VidBuxServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "VidBux";
    }

    @Override
    public String getName() {
        return "vidbux.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VidBuxFileRunner();
    }

}